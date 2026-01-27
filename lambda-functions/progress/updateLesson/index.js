const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand, UpdateCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({ region: "us-east-1" });
const docClient = DynamoDBDocumentClient.from(client);

/**
 * Update lesson progress
 * Awards XP for lesson completion
 */
exports.handler = async (event) => {
    try {
        const userId = event.requestContext?.authorizer?.claims?.sub;

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
            TableName: process.env.DYNAMODB_TABLE,
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

        // If lesson completed, award XP
        if (completed) {
            const xpEarned = XP_REWARDS.MODULE_COMPLETED;

            // Get current user profile
            const userResult = await docClient.send(new GetCommand({
                TableName: process.env.DYNAMODB_TABLE,
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
                const newLevel = Math.floor(newTotalXP / 500) + 1;

                // Update user profile with new XP
                await docClient.send(new UpdateCommand({
                    TableName: process.env.DYNAMODB_TABLE,
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
                xpEarned: completed ? XP_REWARDS.MODULE_COMPLETED : 0
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
