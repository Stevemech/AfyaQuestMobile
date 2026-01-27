/**
 * Lambda Function: auth-register
 * Creates a new user in Cognito and DynamoDB
 */

const { CognitoIdentityProviderClient, SignUpCommand } = require("@aws-sdk/client-cognito-identity-provider");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand } = require("@aws-sdk/lib-dynamodb");
const { randomUUID } = require("crypto");

// All services in us-east-1
const cognitoClient = new CognitoIdentityProviderClient({ region: 'us-east-1' });
const dynamoClient = new DynamoDBClient({ region: 'us-east-1' });
const docClient = DynamoDBDocumentClient.from(dynamoClient);

exports.handler = async (event) => {
    console.log('Register request:', JSON.stringify(event, null, 2));

    try {
        const body = JSON.parse(event.body);
        const { email, password, name, phone, role = 'cha' } = body;

        // Validate input
        if (!email || !password || !name) {
            return {
                statusCode: 400,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ error: 'Email, password, and name are required' })
            };
        }

        // Create user in Cognito (only standard attributes)
        const signUpParams = {
            ClientId: process.env.COGNITO_CLIENT_ID,
            Username: email,
            Password: password,
            UserAttributes: [
                { Name: 'email', Value: email },
                { Name: 'name', Value: name }
            ]
        };

        // Add phone number if provided
        if (phone) {
            signUpParams.UserAttributes.push({ Name: 'phone_number', Value: phone });
        }

        const signUpCommand = new SignUpCommand(signUpParams);
        const cognitoResponse = await cognitoClient.send(signUpCommand);

        const userId = cognitoResponse.UserSub;

        // Create user profile in DynamoDB
        const userProfile = {
            PK: `USER#${userId}`,
            SK: 'PROFILE',
            id: userId,
            email,
            name,
            phone: phone || null,
            role,
            language: 'en',
            level: 1,
            totalPoints: 0,
            rank: 'Bronze CHA',
            currentStreak: 0,
            isActive: true,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        };

        const putCommand = new PutCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Item: userProfile
        });

        await docClient.send(putCommand);

        return {
            statusCode: 201,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({
                message: 'User registered successfully',
                userId,
                userConfirmed: cognitoResponse.UserConfirmed,
                codeDeliveryDetails: cognitoResponse.CodeDeliveryDetails
            })
        };

    } catch (error) {
        console.error('Registration error:', error);

        let statusCode = 500;
        let errorMessage = 'Internal server error';

        if (error.name === 'UsernameExistsException') {
            statusCode = 409;
            errorMessage = 'User already exists';
        } else if (error.name === 'InvalidPasswordException') {
            statusCode = 400;
            errorMessage = 'Password does not meet requirements';
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
