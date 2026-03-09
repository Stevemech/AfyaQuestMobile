/**
 * Lambda: auth-forgotPassword
 * Initiates Cognito forgot password flow — sends verification code to user's email.
 */
const { CognitoIdentityProviderClient, ForgotPasswordCommand } = require("@aws-sdk/client-cognito-identity-provider");

const cognitoClient = new CognitoIdentityProviderClient({ region: "af-south-1" });
const CLIENT_ID = "3oj94klb6jejp4lbal9ninv870";

exports.handler = async (event) => {
  const headers = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type,Authorization",
    "Content-Type": "application/json",
  };

  try {
    const body = JSON.parse(event.body || "{}");
    const { email } = body;

    if (!email) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: "Email is required" }) };
    }

    await cognitoClient.send(new ForgotPasswordCommand({
      ClientId: CLIENT_ID,
      Username: email,
    }));

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ message: "Verification code sent to your email" }),
    };
  } catch (err) {
    console.error("ForgotPassword error:", err);
    const message = err.name === "UserNotFoundException"
      ? "No account found with that email"
      : err.name === "LimitExceededException"
        ? "Too many attempts. Please try again later"
        : err.message || "Failed to send verification code";
    return { statusCode: 400, headers, body: JSON.stringify({ error: message }) };
  }
};
