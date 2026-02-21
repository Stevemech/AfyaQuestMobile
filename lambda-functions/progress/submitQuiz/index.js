const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand, UpdateCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client);

const MAX_LIVES = 10;

const XP_BY_DIFFICULTY = {
    easy: 10,
    medium: 20,
    hard: 30
};

const XP_REWARDS = {
    DAILY_QUESTION_BONUS: 25 // For perfect score
};

/** Progressive level thresholds matching the mobile app */
const LEVEL_THRESHOLDS = [0, 100, 250, 500, 850, 1300, 1900, 2650, 3550, 4600];
const XP_PER_LEVEL_AFTER_TABLE = 1200;

function calculateLevel(totalXP) {
    for (let i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
        if (totalXP >= LEVEL_THRESHOLDS[i]) {
            if (i === LEVEL_THRESHOLDS.length - 1 && totalXP > LEVEL_THRESHOLDS[i]) {
                const extraXP = totalXP - LEVEL_THRESHOLDS[i];
                return i + Math.floor(extraXP / XP_PER_LEVEL_AFTER_TABLE);
            }
            return i;
        }
    }
    return 0;
}

/**
 * Submit quiz results
 * Awards XP for correct answers and manages lives
 */
exports.handler = async (event) => {
    try {
        const userId = event.requestContext?.authorizer?.jwt?.claims?.sub
            || event.requestContext?.authorizer?.claims?.sub;

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

        const timestamp = new Date().toISOString();
        const score = Math.round((correctAnswers / totalQuestions) * 100);

        // Calculate XP earned based on difficulty of each answer
        let xpEarned = 0;
        if (answers && answers.length > 0) {
            for (const answer of answers) {
                if (answer.isCorrect) {
                    const difficulty = (answer.difficulty || 'medium').toLowerCase();
                    xpEarned += XP_BY_DIFFICULTY[difficulty] || XP_BY_DIFFICULTY.medium;
                }
            }
        } else {
            // Fallback: use medium XP per correct answer
            xpEarned = correctAnswers * XP_BY_DIFFICULTY.medium;
        }

        if (score === 100) {
            xpEarned += XP_REWARDS.DAILY_QUESTION_BONUS;
        }

        // Calculate lives change: +1 per correct, -1 per incorrect
        const livesGained = correctAnswers;
        const livesLost = incorrectAnswers || 0;

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
            const newLevel = calculateLevel(newTotalXP);
            const newLives = Math.min(Math.max(currentLives + livesGained - livesLost, 0), MAX_LIVES);

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
