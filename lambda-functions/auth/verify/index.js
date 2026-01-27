/**
 * Lambda Function: auth-verify
 * Confirms user email verification via link/code
 */

const { CognitoIdentityProviderClient, ConfirmSignUpCommand } = require("@aws-sdk/client-cognito-identity-provider");

const cognitoClient = new CognitoIdentityProviderClient({ region: 'us-east-1' });

exports.handler = async (event) => {
    console.log('Verify request:', JSON.stringify(event, null, 2));

    try {
        // Get parameters from query string (for GET requests with link)
        const queryParams = event.queryStringParameters || {};
        const { email, code } = queryParams;

        // Validate input
        if (!email || !code) {
            return {
                statusCode: 400,
                headers: {
                    'Content-Type': 'text/html',
                    'Access-Control-Allow-Origin': '*'
                },
                body: `
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>Verification Failed</title>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <style>
                            body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }
                            .container { background: white; padding: 30px; border-radius: 10px; max-width: 500px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                            .error { color: #d32f2f; }
                            h1 { color: #333; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <h1 class="error">❌ Verification Failed</h1>
                            <p>Invalid verification link. Email and verification code are required.</p>
                            <p>Please try registering again or contact support.</p>
                        </div>
                    </body>
                    </html>
                `
            };
        }

        // Confirm user sign up in Cognito
        const confirmParams = {
            ClientId: process.env.COGNITO_CLIENT_ID,
            Username: email,
            ConfirmationCode: code
        };

        const confirmCommand = new ConfirmSignUpCommand(confirmParams);
        await cognitoClient.send(confirmCommand);

        // Return success page
        return {
            statusCode: 200,
            headers: {
                'Content-Type': 'text/html',
                'Access-Control-Allow-Origin': '*'
            },
            body: `
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Email Verified</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }
                        .container { background: white; padding: 30px; border-radius: 10px; max-width: 500px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .success { color: #4caf50; }
                        h1 { color: #333; }
                        .button {
                            display: inline-block;
                            background: #4caf50;
                            color: white;
                            padding: 12px 30px;
                            border-radius: 5px;
                            text-decoration: none;
                            margin-top: 20px;
                            font-weight: bold;
                        }
                        .app-link {
                            margin-top: 20px;
                            padding: 15px;
                            background: #e3f2fd;
                            border-radius: 5px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1 class="success">✅ Email Verified!</h1>
                        <p>Your email has been successfully verified.</p>
                        <p>You can now log in to AfyaQuest with your credentials.</p>

                        <div class="app-link">
                            <p><strong>Open the AfyaQuest app to continue</strong></p>
                            <a href="afyaquest://verified" class="button">Open AfyaQuest App</a>
                        </div>

                        <p style="margin-top: 20px; color: #666; font-size: 14px;">
                            If the button doesn't work, please open the AfyaQuest app manually.
                        </p>
                    </div>
                </body>
                </html>
            `
        };

    } catch (error) {
        console.error('Verification error:', error);

        let errorMessage = 'An error occurred during verification. Please try again.';

        if (error.name === 'CodeMismatchException') {
            errorMessage = 'Invalid verification code. The code may have expired or is incorrect.';
        } else if (error.name === 'ExpiredCodeException') {
            errorMessage = 'Verification code has expired. Please request a new verification code.';
        } else if (error.name === 'UserNotFoundException') {
            errorMessage = 'User not found. Please check your email address.';
        } else if (error.name === 'NotAuthorizedException') {
            errorMessage = 'User is already confirmed or the verification code is invalid.';
        }

        return {
            statusCode: 400,
            headers: {
                'Content-Type': 'text/html',
                'Access-Control-Allow-Origin': '*'
            },
            body: `
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Verification Failed</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }
                        .container { background: white; padding: 30px; border-radius: 10px; max-width: 500px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .error { color: #d32f2f; }
                        h1 { color: #333; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1 class="error">❌ Verification Failed</h1>
                        <p>${errorMessage}</p>
                        <p style="margin-top: 20px; color: #666; font-size: 14px;">
                            Error details: ${error.message}
                        </p>
                    </div>
                </body>
                </html>
            `
        };
    }
};
