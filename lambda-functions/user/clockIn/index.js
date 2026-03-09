const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, UpdateCommand, PutCommand } = require('@aws-sdk/lib-dynamodb');

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
