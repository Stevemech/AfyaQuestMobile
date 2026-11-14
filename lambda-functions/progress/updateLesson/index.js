const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand, UpdateCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client);
const TABLE = "AfyaQuestData";

/** Progressive level thresholds: same table as progress-submitQuiz and the mobile app. */
const LEVEL_THRESHOLDS = [0, 100, 250, 500, 850, 1300, 1900, 2650, 3550, 4600];
const XP_PER_LEVEL_AFTER_TABLE = 1200;

function calculateLevel(totalXP) {
    for (let i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
        if (totalXP >= LEVEL_THRESHOLDS[i]) {
            if (i === LEVEL_THRESHOLDS.length - 1 && totalXP > LEVEL_THRESHOLDS[i]) {
                return i + Math.floor((totalXP - LEVEL_THRESHOLDS[i]) / XP_PER_LEVEL_AFTER_TABLE);
            }
            return i;
        }
    }
    return 0;
}

/**
 * Update lesson progress
 * Awards XP for lesson completion
 */
exports.handler = async (event) => {
    try {
        const userId = event.requestContext?.authorizer?.jwt?.claims?.sub;

        if (!userId) {
            return {
                statusCode: 401,
                headers: {
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Allow-Headers": "*"
                },
                body: JSON.stringify({ error: "Unauthorized" })
            };
        }

        const body = JSON.parse(event.body);
        const { lessonId, completed, score } = body;

        if (!lessonId) {
            return {
                statusCode: 400,
                headers: {
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Allow-Headers": "*"
                },
                body: JSON.stringify({ error: "lessonId is required" })
            };
        }

        // XP rewards
        const XP_REWARDS = {
            MODULE_COMPLETED: 75,
            VIDEO_WATCHED: 20
        };

        const timestamp = new Date().toISOString();

        // Save lesson progress
        await docClient.send(new PutCommand({
            TableName: TABLE,
            Item: {
                PK: `USER#${userId}`,
                SK: `PROGRESS#${lessonId}`,
                lessonId,
                completed: completed || false,
                score: score || 0,
                completedDate: timestamp,
                updatedAt: timestamp
            }
        }));

        // If lesson completed, award XP. Newer app versions award it on the device and upload
        // their totals through "state_sync"; they send skipXp so it is not counted twice.
        const skipXp = body.skipXp === true;
        if (completed && !skipXp) {
            const xpEarned = XP_REWARDS.MODULE_COMPLETED;

            // Get current user profile
            const userResult = await docClient.send(new GetCommand({
                TableName: TABLE,
                Key: {
                    PK: `USER#${userId}`,
                    SK: "PROFILE"
                }
            }));

            if (userResult.Item) {
                const currentTotalXP = userResult.Item.totalPoints || 0;
                const currentDailyXP = userResult.Item.dailyXP || 0;
                const currentWeeklyXP = userResult.Item.weeklyXP || 0;
                const newTotalXP = currentTotalXP + xpEarned;
                const newLevel = calculateLevel(newTotalXP);

                // Update user profile with new XP
                await docClient.send(new UpdateCommand({
                    TableName: TABLE,
                    Key: {
                        PK: `USER#${userId}`,
                        SK: "PROFILE"
                    },
                    UpdateExpression: "SET totalPoints = :totalPoints, dailyXP = :dailyXP, weeklyXP = :weeklyXP, #level = :level, updatedAt = :updatedAt",
                    ExpressionAttributeNames: {
                        "#level": "level"
                    },
                    ExpressionAttributeValues: {
                        ":totalPoints": newTotalXP,
                        ":dailyXP": currentDailyXP + xpEarned,
                        ":weeklyXP": currentWeeklyXP + xpEarned,
                        ":level": newLevel,
                        ":updatedAt": timestamp
                    }
                }));
            }
        }

        return {
            statusCode: 200,
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Headers": "*"
            },
            body: JSON.stringify({
                message: "Lesson progress updated",
                lessonId,
                completed,
                xpEarned: completed && !skipXp ? XP_REWARDS.MODULE_COMPLETED : 0
            })
        };

    } catch (error) {
        console.error('Error updating lesson progress:', error);
        return {
            statusCode: 500,
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Headers": "*"
            },
            body: JSON.stringify({ error: "Failed to update lesson progress" })
        };
    }
};
