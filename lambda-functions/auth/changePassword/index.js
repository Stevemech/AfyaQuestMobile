/**
 * Lambda Function: afyaquest-auth-changePassword
 * Changes the authenticated user's password.
 *
 * The mobile client sends its ID token (ProfileViewModel.changePassword), but
 * Cognito's ChangePassword API only accepts an ACCESS token. So instead of
 * forwarding the caller's token we re-verify the current password with
 * InitiateAuth and then set the new one with AdminSetUserPassword. The route
 * is already behind the JWT authorizer, so the caller's identity is trusted;
 * verifying currentPassword is what stops a stolen token from silently
 * changing a password.
 */
const {
    CognitoIdentityProviderClient,
    InitiateAuthCommand,
    AdminSetUserPasswordCommand,
} = require("@aws-sdk/client-cognito-identity-provider");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, GetCommand } = require("@aws-sdk/lib-dynamodb");

const cognito = new CognitoIdentityProviderClient({ region: process.env.AWS_REGION });
const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({ region: process.env.AWS_REGION }));
const TABLE = process.env.DYNAMODB_TABLE || "AfyaQuestData";

exports.handler = async (event) => {
    const headers = {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "Content-Type,Authorization",
        "Content-Type": "application/json",
    };

    try {
        const claims = event.requestContext?.authorizer?.jwt?.claims
            || event.requestContext?.authorizer?.claims;
        const userId = claims?.sub;
        if (!userId) {
            return { statusCode: 401, headers, body: JSON.stringify({ error: "Unauthorized" }) };
        }

        const body = JSON.parse(event.body || "{}");
        const { currentPassword, newPassword } = body;
        if (!currentPassword || !newPassword) {
            return {
                statusCode: 400, headers,
                body: JSON.stringify({ error: "currentPassword and newPassword are required" }),
            };
        }
        if (currentPassword === newPassword) {
            return {
                statusCode: 400, headers,
                body: JSON.stringify({ error: "New password must be different from the current one" }),
            };
        }

        // Access tokens carry no email claim, so fall back to the stored profile.
        let email = claims.email;
        if (!email) {
            const prof = await ddb.send(new GetCommand({
                TableName: TABLE,
                Key: { PK: `USER#${userId}`, SK: "PROFILE" },
            }));
            email = prof.Item?.email;
        }
        if (!email) {
            return { statusCode: 404, headers, body: JSON.stringify({ error: "User profile not found" }) };
        }

        try {
            await cognito.send(new InitiateAuthCommand({
                AuthFlow: "USER_PASSWORD_AUTH",
                ClientId: process.env.COGNITO_CLIENT_ID,
                AuthParameters: { USERNAME: email, PASSWORD: currentPassword },
            }));
        } catch (err) {
            if (err.name === "NotAuthorizedException") {
                return {
                    statusCode: 401, headers,
                    body: JSON.stringify({ error: "Current password is incorrect" }),
                };
            }
            throw err;
        }

        await cognito.send(new AdminSetUserPasswordCommand({
            UserPoolId: process.env.COGNITO_USER_POOL_ID,
            Username: email,
            Password: newPassword,
            Permanent: true,
        }));

        return { statusCode: 200, headers, body: JSON.stringify({ message: "Password changed successfully" }) };
    } catch (err) {
        console.error("changePassword error:", err);
        if (err.name === "InvalidPasswordException") {
            return { statusCode: 400, headers, body: JSON.stringify({ error: err.message }) };
        }
        return { statusCode: 500, headers, body: JSON.stringify({ error: "Failed to change password" }) };
    }
};
