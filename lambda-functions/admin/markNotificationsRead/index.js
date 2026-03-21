const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, UpdateCommand } = require('@aws-sdk/lib-dynamodb');

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
    const body = JSON.parse(event.body || '{}');
    const org = body.organization || event.queryStringParameters?.organization;
    const { sks } = body;

    if (!org) {
      return {
        statusCode: 400,
        headers,
        body: JSON.stringify({ error: 'organization is required' }),
      };
    }

    if (!Array.isArray(sks) || sks.length === 0) {
      return {
        statusCode: 400,
        headers,
        body: JSON.stringify({ error: 'sks must be a non-empty array of notification sort keys' }),
      };
    }

    const pk = `ORG#${org}`;
    for (const sk of sks) {
      if (typeof sk !== 'string' || !sk.startsWith('NOTIF#')) {
        continue;
      }
      try {
        await ddb.send(new UpdateCommand({
          TableName: TABLE,
          Key: { PK: pk, SK: sk },
          UpdateExpression: 'SET #r = :true, readAt = :ts',
          ExpressionAttributeNames: { '#r': 'read' },
          ExpressionAttributeValues: {
            ':true': true,
            ':ts': new Date().toISOString(),
          },
        }));
      } catch (e) {
        console.warn('mark read skipped for', sk, e.message);
      }
    }

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ message: 'Updated' }),
    };
  } catch (err) {
    console.error('markNotificationsRead error:', err);
    return {
      statusCode: 500,
      headers,
      body: JSON.stringify({ error: 'Failed to mark notifications read' }),
    };
  }
};
