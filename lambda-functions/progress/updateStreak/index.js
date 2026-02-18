const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, UpdateCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client);

/**
 * Update user streak
 * Awards bonus XP for maintaining streaks
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
        const { increment } = body; // true to increment, false to reset

        const XP_REWARDS = {
            STREAK_BONUS: 25 // Per day of streak
        };

        // Get current user profile
        const userResult = await docClient.send(new GetCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Key: {
                PK: `USER#${userId}`,
                SK: "PROFILE"
            }
        }));

        if (!userResult.Item) {
            return {
                statusCode: 404,
                headers: {
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Allow-Headers": "*"
                },
                body: JSON.stringify({ error: "User not found" })
            };
        }

        const currentStreak = userResult.Item.currentStreak || 0;
        const currentTotalXP = userResult.Item.totalPoints || 0;
        const currentDailyXP = userResult.Item.dailyXP || 0;
        const currentWeeklyXP = userResult.Item.weeklyXP || 0;

        let newStreak, xpEarned = 0, newTotalXP = currentTotalXP;

        if (increment) {
            newStreak = currentStreak + 1;
            xpEarned = XP_REWARDS.STREAK_BONUS * newStreak;
            newTotalXP = currentTotalXP + xpEarned;
        } else {
            newStreak = 0;
        }

        const newLevel = Math.floor(newTotalXP / 500) + 1;
        const timestamp = new Date().toISOString();

        // Update user streak and XP
        await docClient.send(new UpdateCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Key: {
                PK: `USER#${userId}`,
                SK: "PROFILE"
            },
            UpdateExpression: "SET currentStreak = :streak, totalPoints = :totalPoints, dailyXP = :dailyXP, weeklyXP = :weeklyXP, #level = :level, lastStreakUpdate = :timestamp, updatedAt = :updatedAt",
            ExpressionAttributeNames: {
                "#level": "level"
            },
            ExpressionAttributeValues: {
                ":streak": newStreak,
                ":totalPoints": newTotalXP,
                ":dailyXP": increment ? currentDailyXP + xpEarned : currentDailyXP,
                ":weeklyXP": increment ? currentWeeklyXP + xpEarned : currentWeeklyXP,
                ":level": newLevel,
                ":timestamp": timestamp,
                ":updatedAt": timestamp
            }
        }));

        return {
            statusCode: 200,
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Headers": "*"
            },
            body: JSON.stringify({
                message: increment ? "Streak updated" : "Streak reset",
                newStreak,
                xpEarned,
                newTotalXP,
                newLevel
            })
        };

    } catch (error) {
        console.error('Error updating streak:', error);
        return {
            statusCode: 500,
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Headers": "*"
            },
            body: JSON.stringify({ error: "Failed to update streak" })
        };
    }
};
