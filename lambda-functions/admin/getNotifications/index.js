const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, QueryCommand } = require('@aws-sdk/lib-dynamodb');
const { requireAdmin } = require('./authz');

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const ddb = DynamoDBDocumentClient.from(client);
const TABLE = 'AfyaQuestData';

exports.handler = async (event) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type,Authorization',
    'Content-Type': 'application/json',
  };

  try {
    // Admin-only: the JWT authorizer proves identity, not role.
    const auth = await requireAdmin(event);
    if (auth.denied) return auth.denied;

    const org = auth.organization;
    if (!org) {
      return {
        statusCode: 400,
        headers,
        body: JSON.stringify({ error: 'organization query parameter is required' }),
      };
    }

    const result = await ddb.send(new QueryCommand({
      TableName: TABLE,
      KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
      ExpressionAttributeValues: {
        ':pk': `ORG#${org}`,
        ':sk': 'NOTIF#',
      },
      Limit: 100,
      ScanIndexForward: false,
    }));

    const notifications = (result.Items || []).map((item) => ({
      id: item.id,
      sk: item.SK,
      type: item.type,
      meta: item.meta || {},
      chvId: item.chvId,
      chvName: item.chvName,
      createdAt: item.createdAt,
      read: !!item.read,
    }));

    const unreadCount = notifications.filter((n) => !n.read).length;

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ notifications, unreadCount }),
    };
  } catch (err) {
    console.error('getNotifications error:', err);
    return {
      statusCode: 500,
      headers,
      body: JSON.stringify({ error: 'Failed to fetch notifications' }),
    };
  }
};
