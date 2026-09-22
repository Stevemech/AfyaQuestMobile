/**
 * Lambda Function: afyaquest-chat-getHistory
 * Returns the authenticated user's chat history with Fred.
 *
 * Items are written by chat-sendMessage as USER#{sub} / CHAT#{timestamp}#{id}.
 * The mobile client types this as List<Map<String, Any>>, so the body is a
 * bare JSON array, not an envelope object.
 */
const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, QueryCommand } = require('@aws-sdk/lib-dynamodb');

const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({ region: process.env.AWS_REGION }));
const TABLE = process.env.DYNAMODB_TABLE || 'AfyaQuestData';

exports.handler = async (event) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type,Authorization',
    'Content-Type': 'application/json',
  };

  try {
    const userId = event.requestContext?.authorizer?.jwt?.claims?.sub
      || event.requestContext?.authorizer?.claims?.sub;
    if (!userId) {
      return { statusCode: 401, headers, body: JSON.stringify({ error: 'Unauthorized' }) };
    }

    const limit = Math.min(parseInt(event.queryStringParameters?.limit || '50', 10) || 50, 200);

    const result = await ddb.send(new QueryCommand({
      TableName: TABLE,
      KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
      ExpressionAttributeValues: { ':pk': `USER#${userId}`, ':sk': 'CHAT#' },
      ScanIndexForward: false, // newest first
      Limit: limit,
    }));

    // Oldest-first so the client can append straight into a conversation view.
    const messages = (result.Items || []).reverse().map(({ PK, SK, ...rest }) => rest);

    return { statusCode: 200, headers, body: JSON.stringify(messages) };
  } catch (err) {
    console.error('getChatHistory error:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch chat history' }) };
  }
};
