/**
 * Lambda Function: auth-getCurrentUser
 * Gets current user profile from DynamoDB
 */

const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, GetCommand } = require("@aws-sdk/lib-dynamodb");

const dynamoClient = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(dynamoClient);

exports.handler = async (event) => {
    console.log('Get current user request:', JSON.stringify(event, null, 2));

    try {
        // Extract user ID from Cognito authorizer
        const userId = event.requestContext?.authorizer?.claims?.sub;

        if (!userId) {
            return {
                statusCode: 401,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ error: 'Unauthorized' })
            };
        }

        // Get user profile from DynamoDB
        const getCommand = new GetCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Key: {
                PK: `USER#${userId}`,
                SK: 'PROFILE'
            }
        });

        const result = await docClient.send(getCommand);

        if (!result.Item) {
            return {
                statusCode: 404,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ error: 'User profile not found' })
            };
        }

        return {
            statusCode: 200,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify(result.Item)
        };

    } catch (error) {
        console.error('Get current user error:', error);

        return {
            statusCode: 500,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({ error: 'Internal server error', details: error.message })
        };
    }
};
