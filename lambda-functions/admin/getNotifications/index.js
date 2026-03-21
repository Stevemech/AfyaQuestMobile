const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, QueryCommand } = require('@aws-sdk/lib-dynamodb');

const client = new DynamoDBClient({ region: 'af-south-1' });
const ddb = DynamoDBDocumentClient.from(client);
const TABLE = 'AfyaQuestData';

exports.handler = async (event) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type,Authorization',
    'Content-Type': 'application/json',
  };

  try {
    const org = event.queryStringParameters?.organization;
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
