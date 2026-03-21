const { randomUUID } = require('crypto');
const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, UpdateCommand, PutCommand, GetCommand } = require('@aws-sdk/lib-dynamodb');

const client = new DynamoDBClient({ region: 'af-south-1' });
const ddb = DynamoDBDocumentClient.from(client);
const TABLE = 'AfyaQuestData';

async function emitOrgNotification(userId, notifType, meta) {
  try {
    const prof = await ddb.send(new GetCommand({
      TableName: TABLE,
      Key: { PK: `USER#${userId}`, SK: 'PROFILE' },
    }));
    const org = prof.Item?.organization;
    if (!org) return;
    const ts = new Date().toISOString();
    const id = randomUUID();
    const sk = `NOTIF#${ts}#${id}`;
    await ddb.send(new PutCommand({
      TableName: TABLE,
      Item: {
        PK: `ORG#${org}`,
        SK: sk,
        id,
        type: notifType,
        meta: meta || {},
        chvId: userId,
        chvName: prof.Item?.name || 'Unknown',
        createdAt: ts,
        read: false,
      },
    }));
  } catch (e) {
    console.warn('emitOrgNotification failed (non-fatal):', e);
  }
}

exports.handler = async (event) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type,Authorization',
    'Content-Type': 'application/json',
  };

  try {
    const userId = event.requestContext?.authorizer?.jwt?.claims?.sub;
    if (!userId) {
      return { statusCode: 401, headers, body: JSON.stringify({ error: 'Unauthorized' }) };
    }

    const body = JSON.parse(event.body || '{}');
    const { action } = body;

    if (!action || !['clock_in', 'clock_out'].includes(action)) {
      return {
        statusCode: 400,
        headers,
        body: JSON.stringify({ error: 'Invalid action. Must be clock_in or clock_out' }),
      };
    }

    const timestamp = new Date().toISOString();
    const dateKey = timestamp.slice(0, 10);

    // Store the clock event record
    await ddb.send(new PutCommand({
      TableName: TABLE,
      Item: {
        PK: `USER#${userId}`,
        SK: `CLOCK#${timestamp}`,
        action,
        timestamp,
        date: dateKey,
      },
    }));

    // Update user profile with manual status
    const manualStatus = action === 'clock_in' ? 'active' : 'inactive';
    const updateExpr = action === 'clock_in'
      ? 'SET manualStatus = :status, lastClockIn = :ts, updatedAt = :ts, lastActiveDate = :ts'
      : 'SET manualStatus = :status, lastClockOut = :ts, updatedAt = :ts';

    await ddb.send(new UpdateCommand({
      TableName: TABLE,
      Key: { PK: `USER#${userId}`, SK: 'PROFILE' },
      UpdateExpression: updateExpr,
      ExpressionAttributeValues: {
        ':status': manualStatus,
        ':ts': timestamp,
      },
    }));

    await emitOrgNotification(userId, action === 'clock_in' ? 'clock_in' : 'clock_out', { date: dateKey });

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({
        message: `Successfully ${action === 'clock_in' ? 'clocked in' : 'clocked out'}`,
        manualStatus,
        timestamp,
      }),
    };
  } catch (err) {
    console.error('Error processing clock action:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to process clock action' }) };
  }
};
