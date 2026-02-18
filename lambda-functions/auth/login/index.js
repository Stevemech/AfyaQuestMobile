/**
 * Lambda Function: auth-login
 * Authenticates user with Cognito and returns JWT tokens
 */

const { CognitoIdentityProviderClient, InitiateAuthCommand } = require("@aws-sdk/client-cognito-identity-provider");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, GetCommand, UpdateCommand } = require("@aws-sdk/lib-dynamodb");

const cognitoClient = new CognitoIdentityProviderClient({ region: process.env.AWS_REGION });
const dynamoClient = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(dynamoClient);

exports.handler = async (event) => {
    console.log('Login request:', JSON.stringify(event, null, 2));

    try {
        const body = JSON.parse(event.body);
        const { email, password } = body;

        if (!email || !password) {
            return {
                statusCode: 400,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ error: 'Email and password are required' })
            };
        }

        // Authenticate with Cognito
        const authParams = {
            AuthFlow: 'USER_PASSWORD_AUTH',
            ClientId: process.env.COGNITO_CLIENT_ID,
            AuthParameters: {
                USERNAME: email,
                PASSWORD: password
            }
        };

        const authCommand = new InitiateAuthCommand(authParams);
        const authResponse = await cognitoClient.send(authCommand);

        if (!authResponse.AuthenticationResult) {
            return {
                statusCode: 401,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ error: 'Authentication failed' })
            };
        }

        // Extract user ID from ID token
        const idToken = authResponse.AuthenticationResult.IdToken;
        const payload = JSON.parse(Buffer.from(idToken.split('.')[1], 'base64').toString());
        const userId = payload.sub;

        // Update last active date in DynamoDB
        const updateCommand = new UpdateCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Key: {
                PK: `USER#${userId}`,
                SK: 'PROFILE'
            },
            UpdateExpression: 'SET lastActiveDate = :date, updatedAt = :date',
            ExpressionAttributeValues: {
                ':date': new Date().toISOString()
            }
        });

        await docClient.send(updateCommand);

        // Get user profile
        const getCommand = new GetCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Key: {
                PK: `USER#${userId}`,
                SK: 'PROFILE'
            }
        });

        const userResult = await docClient.send(getCommand);

        return {
            statusCode: 200,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({
                accessToken: authResponse.AuthenticationResult.AccessToken,
                idToken: authResponse.AuthenticationResult.IdToken,
                refreshToken: authResponse.AuthenticationResult.RefreshToken,
                expiresIn: authResponse.AuthenticationResult.ExpiresIn,
                user: userResult.Item
            })
        };

    } catch (error) {
        console.error('Login error:', error);

        let statusCode = 500;
        let errorMessage = 'Internal server error';

        if (error.name === 'NotAuthorizedException') {
            statusCode = 401;
            errorMessage = 'Invalid email or password';
        } else if (error.name === 'UserNotFoundException') {
            statusCode = 404;
            errorMessage = 'User not found';
        } else if (error.name === 'UserNotConfirmedException') {
            statusCode = 403;
            errorMessage = 'User email not confirmed';
        }

        return {
            statusCode,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({ error: errorMessage, details: error.message })
        };
    }
};
