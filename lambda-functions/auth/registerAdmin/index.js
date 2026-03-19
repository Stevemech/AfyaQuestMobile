/**
 * Lambda: auth-registerAdmin
 * Registers a new admin user requesting a new organization.
 * The user is created in Cognito but immediately disabled — an administrator
 * must enable them through the AWS Cognito console before they can sign in.
 * An SNS notification is published so the admin receives an email.
 */

const { CognitoIdentityProviderClient, SignUpCommand, AdminDisableUserCommand } = require("@aws-sdk/client-cognito-identity-provider");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand } = require("@aws-sdk/lib-dynamodb");
const { SNSClient, PublishCommand } = require("@aws-sdk/client-sns");

const region = process.env.AWS_REGION || "af-south-1";
const cognitoClient = new CognitoIdentityProviderClient({ region });
const dynamoClient = new DynamoDBClient({ region });
const docClient = DynamoDBDocumentClient.from(dynamoClient);
const snsClient = new SNSClient({ region });

const headers = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "Content-Type,Authorization",
  "Content-Type": "application/json",
};

exports.handler = async (event) => {
  console.log("RegisterAdmin request:", JSON.stringify(event, null, 2));

  try {
    const body = JSON.parse(event.body || "{}");
    const { email, password, name, phone, organizationName } = body;

    if (!email || !password || !name || !organizationName) {
      return {
        statusCode: 400,
        headers,
        body: JSON.stringify({ error: "Email, password, name, and organization name are required" }),
      };
    }

    // 1. Create user in Cognito
    const signUpResponse = await cognitoClient.send(new SignUpCommand({
      ClientId: process.env.COGNITO_CLIENT_ID,
      Username: email,
      Password: password,
      UserAttributes: [
        { Name: "email", Value: email },
        { Name: "name", Value: name },
        ...(phone ? [{ Name: "phone_number", Value: phone }] : []),
      ],
    }));

    const userId = signUpResponse.UserSub;

    // 2. Immediately disable the user so they cannot log in until approved
    try {
      await cognitoClient.send(new AdminDisableUserCommand({
        UserPoolId: process.env.COGNITO_USER_POOL_ID,
        Username: email,
      }));
    } catch (disableErr) {
      console.error("Failed to disable user (will proceed):", disableErr);
    }

    // 3. Create user profile in DynamoDB with pendingApproval flag
    const now = new Date().toISOString();
    await docClient.send(new PutCommand({
      TableName: process.env.DYNAMODB_TABLE,
      Item: {
        PK: `USER#${userId}`,
        SK: "PROFILE",
        id: userId,
        email,
        name,
        phone: phone || null,
        role: "admin",
        organization: organizationName,
        pendingApproval: true,
        language: "en",
        level: 0,
        totalPoints: 0,
        lives: 10,
        dailyXP: 0,
        weeklyXP: 0,
        rank: "Beginner",
        currentStreak: 0,
        isActive: false,
        createdAt: now,
        updatedAt: now,
      },
    }));

    // 4. Send SNS notification to admin (best-effort)
    if (process.env.SNS_TOPIC_ARN) {
      try {
        await snsClient.send(new PublishCommand({
          TopicArn: process.env.SNS_TOPIC_ARN,
          Subject: "AfyaQuest — New Organization Request",
          Message: [
            "A new organization has been requested on AfyaQuest.",
            "",
            `  Name:         ${name}`,
            `  Email:        ${email}`,
            `  Organization: ${organizationName}`,
            `  Phone:        ${phone || "N/A"}`,
            `  Date:         ${now}`,
            "",
            "To approve this request:",
            "  1. Open the AWS Cognito console (af-south-1)",
            "  2. Go to the AfyaQuest User Pool → Users",
            `  3. Search for "${email}"`,
            "  4. Click the user and choose Actions → Enable user",
            "",
            "Once enabled, the user will be able to sign in to the admin portal.",
          ].join("\n"),
        }));
      } catch (snsErr) {
        console.error("SNS notification failed (non-fatal):", snsErr);
      }
    }

    return {
      statusCode: 201,
      headers,
      body: JSON.stringify({
        message: "Registration successful. Please verify your email. An administrator will review your request.",
        userId,
      }),
    };
  } catch (err) {
    console.error("RegisterAdmin error:", err);

    let statusCode = 500;
    let message = "Registration failed";

    if (err.name === "UsernameExistsException") {
      statusCode = 409;
      message = "An account with this email already exists";
    } else if (err.name === "InvalidPasswordException") {
      statusCode = 400;
      message = "Password does not meet requirements (min 8 chars, uppercase, lowercase, number)";
    } else if (err.name === "InvalidParameterException") {
      statusCode = 400;
      message = err.message || "Invalid input parameters";
    }

    return { statusCode, headers, body: JSON.stringify({ error: message }) };
  }
};
