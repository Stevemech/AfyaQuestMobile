/**
 * Shared authorization helper for AfyaQuest admin Lambdas.
 *
 * The JWT authorizer on the API only proves the caller is a signed-in user of
 * the pool -- it says nothing about role. Every admin endpoint must therefore
 * check the caller's stored profile itself. Organization is read from that
 * profile too, never from a query parameter, so a caller cannot read another
 * organization's data by changing the URL.
 *
 * This file is copied into each function's bundle at deploy time by deploy.sh;
 * edit it here, not in a function directory.
 */
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, GetCommand } = require("@aws-sdk/lib-dynamodb");

const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({ region: process.env.AWS_REGION }));
const TABLE = process.env.DYNAMODB_TABLE || "AfyaQuestData";

const HEADERS = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type,Authorization",
    "Content-Type": "application/json",
};

function deny(statusCode, error) {
    return { statusCode, headers: HEADERS, body: JSON.stringify({ error }) };
}

/**
 * Resolves the caller and asserts they are an admin.
 *
 * @returns {Promise<{denied?: object, userId?: string, organization?: string, profile?: object}>}
 *   On failure, `denied` is a ready-to-return API Gateway response.
 */
async function requireAdmin(event) {
    const userId = event.requestContext?.authorizer?.jwt?.claims?.sub
        || event.requestContext?.authorizer?.claims?.sub;
    if (!userId) return { denied: deny(401, "Unauthorized") };

    let profile;
    try {
        const res = await ddb.send(new GetCommand({
            TableName: TABLE,
            Key: { PK: `USER#${userId}`, SK: "PROFILE" },
        }));
        profile = res.Item;
    } catch (err) {
        console.error("requireAdmin: profile lookup failed:", err);
        return { denied: deny(500, "Authorization check failed") };
    }

    if (!profile) return { denied: deny(403, "Forbidden") };
    if (profile.role !== "admin") return { denied: deny(403, "Admin role required") };
    if (!profile.organization) {
        // An admin with no organization would otherwise fall through to an
        // unscoped scan and see every organization's data.
        return { denied: deny(403, "Admin account has no organization assigned") };
    }

    return { userId, organization: profile.organization, profile };
}

module.exports = { requireAdmin, HEADERS };
