const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, QueryCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client);

/**
 * Get user progress data
 * Returns: user stats, completed lessons, quiz scores, achievements
 */
exports.handler = async (event) => {
    try {
        // Extract userId from request context (set by Cognito authorizer)
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

        // The five reads do not depend on each other, so they run together. One after the other
        // they made this endpoint, which the app calls on every sign-in and every start, several
        // times slower than it needs to be.
        const table = process.env.DYNAMODB_TABLE;
        const pk = `USER#${userId}`;
        const byPrefix = (prefix, extra = {}) => docClient.send(new QueryCommand({
            TableName: table,
            KeyConditionExpression: "PK = :pk AND begins_with(SK, :sk)",
            ExpressionAttributeValues: { ":pk": pk, ":sk": prefix },
            ...extra
        }));
        const [profileResult, progressResult, achievementsResult, quizResult, stateResult] = await Promise.all([
            docClient.send(new GetCommand({ TableName: table, Key: { PK: pk, SK: "PROFILE" } })),
            byPrefix("PROGRESS#"),
            byPrefix("ACHIEVEMENT#"),
            // Most recent first, last 50 quiz attempts
            byPrefix("QUIZ#", { ScanIndexForward: false, Limit: 50 }),
            // Account-level learning state uploaded by the app (see user-updateProgress "state_sync").
            docClient.send(new GetCommand({ TableName: table, Key: { PK: pk, SK: "STATE" } }))
        ]);

        if (!profileResult.Item) {
            return {
                statusCode: 404,
                headers: {
                    "Access-Control-Allow-Origin": "*",
                    "Access-Control-Allow-Headers": "*"
                },
                body: JSON.stringify({ error: "User not found" })
            };
        }

        let state = null;
        if (stateResult.Item) {
            const { PK, SK, ...rest } = stateResult.Item;
            state = rest;
        }

        // A new account really has 0 XP, level 0 and no streak. The old demo defaults
        // (1432 XP, level 5, 3-day streak) made every empty profile look active.
        const response = {
            profile: {
                userId: profileResult.Item.id || profileResult.Item.userId,
                name: profileResult.Item.name,
                email: profileResult.Item.email,
                level: profileResult.Item.level ?? 0,
                totalPoints: profileResult.Item.totalPoints ?? 0,
                dailyXP: profileResult.Item.dailyXP ?? 0,
                weeklyXP: profileResult.Item.weeklyXP ?? 0,
                rank: profileResult.Item.rank || "Beginner",
                currentStreak: profileResult.Item.currentStreak ?? 0,
                lives: profileResult.Item.lives ?? 10,
                lastResetDate: profileResult.Item.lastResetDate || new Date().toISOString().split('T')[0]
            },
            state,
            progress: progressResult.Items || [],
            achievements: achievementsResult.Items || [],
            recentQuizzes: quizResult.Items || []
        };

        return {
            statusCode: 200,
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Headers": "*"
            },
            body: JSON.stringify(response)
        };

    } catch (error) {
        console.error('Error getting user progress:', error);
        return {
            statusCode: 500,
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Headers": "*"
            },
            body: JSON.stringify({ error: "Failed to get user progress" })
        };
    }
};
