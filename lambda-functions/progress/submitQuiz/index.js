const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand, UpdateCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client);

/**
 * Submit quiz results
 * Awards XP for correct answers and manages lives
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
        const { videoId, totalQuestions, correctAnswers, incorrectAnswers, answers } = body;

        if (!videoId || totalQuestions === undefined || correctAnswers === undefined) {
            return {
                statusCode: 400,
                headers: {
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Allow-Headers": "*"
                },
                body: JSON.stringify({ error: "videoId, totalQuestions, and correctAnswers are required" })
            };
        }

        const XP_REWARDS = {
            DAILY_QUESTION_CORRECT: 30,
            DAILY_QUESTION_BONUS: 50 // For perfect score
        };

        const timestamp = new Date().toISOString();
        const score = Math.round((correctAnswers / totalQuestions) * 100);

        // Calculate XP earned
        let xpEarned = correctAnswers * XP_REWARDS.DAILY_QUESTION_CORRECT;
        if (score === 100) {
            xpEarned += XP_REWARDS.DAILY_QUESTION_BONUS; // Bonus for perfect score
        }

        // Calculate lives change
        const livesGained = correctAnswers * 2; // +2 lives per correct answer
        const livesLost = incorrectAnswers || 0; // -1 life per incorrect answer

        // Save quiz result
        await docClient.send(new PutCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Item: {
                PK: `USER#${userId}`,
                SK: `QUIZ#${videoId}#${timestamp}`,
                videoId,
                totalQuestions,
                correctAnswers,
                incorrectAnswers: incorrectAnswers || 0,
                score,
                answers: answers || [],
                xpEarned,
                livesGained,
                livesLost,
                completedAt: timestamp
            }
        }));

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
            const currentLives = userResult.Item.lives || 10;

            const newTotalXP = currentTotalXP + xpEarned;
            const newLevel = Math.floor(newTotalXP / 500) + 1;
            const newLives = Math.max(currentLives + livesGained - livesLost, 0);

            // Update user profile
            await docClient.send(new UpdateCommand({
                TableName: process.env.DYNAMODB_TABLE,
                Key: {
                    PK: `USER#${userId}`,
                    SK: "PROFILE"
                },
                UpdateExpression: "SET totalPoints = :totalPoints, dailyXP = :dailyXP, weeklyXP = :weeklyXP, #level = :level, lives = :lives, updatedAt = :updatedAt",
                ExpressionAttributeNames: {
                    "#level": "level"
                },
                ExpressionAttributeValues: {
                    ":totalPoints": newTotalXP,
                    ":dailyXP": currentDailyXP + xpEarned,
                    ":weeklyXP": currentWeeklyXP + xpEarned,
                    ":level": newLevel,
                    ":lives": newLives,
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
                    message: "Quiz submitted successfully",
                    score,
                    xpEarned,
                    livesGained,
                    livesLost,
                    newTotalXP,
                    newLevel,
                    newLives
                })
            };
        } else {
            return {
                statusCode: 404,
                headers: {
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Allow-Headers": "*"
                },
                body: JSON.stringify({ error: "User not found" })
            };
        }

    } catch (error) {
        console.error('Error submitting quiz:', error);
        return {
            statusCode: 500,
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Headers": "*"
            },
            body: JSON.stringify({ error: "Failed to submit quiz" })
        };
    }
};
