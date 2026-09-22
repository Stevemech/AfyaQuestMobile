/**
 * Lambda: auth-confirmForgotPassword
 * Completes Cognito forgot password flow — verifies code and sets new password.
 */
const { CognitoIdentityProviderClient, ConfirmForgotPasswordCommand } = require("@aws-sdk/client-cognito-identity-provider");

const cognitoClient = new CognitoIdentityProviderClient({ region: process.env.AWS_REGION });
const CLIENT_ID = process.env.COGNITO_CLIENT_ID;

exports.handler = async (event) => {
  const headers = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type,Authorization",
    "Content-Type": "application/json",
  };

  try {
    const body = JSON.parse(event.body || "{}");
    const { email, code, newPassword } = body;

    if (!email || !code || !newPassword) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: "Email, code, and newPassword are required" }) };
    }

    await cognitoClient.send(new ConfirmForgotPasswordCommand({
      ClientId: CLIENT_ID,
      Username: email,
      ConfirmationCode: code,
      Password: newPassword,
    }));

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ message: "Password reset successfully" }),
    };
  } catch (err) {
    console.error("ConfirmForgotPassword error:", err);
    const message = err.name === "CodeMismatchException"
      ? "Invalid verification code"
      : err.name === "ExpiredCodeException"
        ? "Verification code has expired. Please request a new one"
        : err.name === "InvalidPasswordException"
          ? "Password does not meet requirements (min 8 chars, uppercase, lowercase, number)"
          : err.message || "Failed to reset password";
    return { statusCode: 400, headers, body: JSON.stringify({ error: message }) };
  }
};
