const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, QueryCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");

const client = new DynamoDBClient({ region: "us-east-1" });
const docClient = DynamoDBDocumentClient.from(client);

/**
 * Get user progress data
 * Returns: user stats, completed lessons, quiz scores, achievements
 */
exports.handler = async (event) => {
    try {
        // Extract userId from request context (set by Cognito authorizer)
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

        // Get user profile
        const profileResult = await docClient.send(new GetCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Key: {
                PK: `USER#${userId}`,
                SK: "PROFILE"
            }
        }));

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

        // Get user progress records
        const progressResult = await docClient.send(new QueryCommand({
            TableName: process.env.DYNAMODB_TABLE,
            KeyConditionExpression: "PK = :pk AND begins_with(SK, :sk)",
            ExpressionAttributeValues: {
                ":pk": `USER#${userId}`,
                ":sk": "PROGRESS#"
            }
        }));

        // Get achievements
        const achievementsResult = await docClient.send(new QueryCommand({
            TableName: process.env.DYNAMODB_TABLE,
            KeyConditionExpression: "PK = :pk AND begins_with(SK, :sk)",
            ExpressionAttributeValues: {
                ":pk": `USER#${userId}`,
                ":sk": "ACHIEVEMENT#"
            }
        }));

        // Get quiz results
        const quizResult = await docClient.send(new QueryCommand({
            TableName: process.env.DYNAMODB_TABLE,
            KeyConditionExpression: "PK = :pk AND begins_with(SK, :sk)",
            ExpressionAttributeValues: {
                ":pk": `USER#${userId}`,
                ":sk": "QUIZ#"
            },
            ScanIndexForward: false, // Sort descending (most recent first)
            Limit: 50 // Last 50 quiz attempts
        }));

        const response = {
            profile: {
                userId: profileResult.Item.userId,
                name: profileResult.Item.name,
                email: profileResult.Item.email,
                level: profileResult.Item.level || 5,
                totalPoints: profileResult.Item.totalPoints || 1432,
                dailyXP: profileResult.Item.dailyXP || 0,
                weeklyXP: profileResult.Item.weeklyXP || 0,
                rank: profileResult.Item.rank || "Novice",
                currentStreak: profileResult.Item.currentStreak || 3,
                lives: profileResult.Item.lives || 10,
                lastResetDate: profileResult.Item.lastResetDate || new Date().toISOString().split('T')[0]
            },
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
