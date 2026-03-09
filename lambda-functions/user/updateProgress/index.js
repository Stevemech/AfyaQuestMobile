/**
 * Lambda Function: user-updateProgress
 * Updates user progress for modules, lessons, and itinerary stops
 */
const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, UpdateCommand } = require('@aws-sdk/lib-dynamodb');

const client = new DynamoDBClient({ region: 'af-south-1' });
const ddb = DynamoDBDocumentClient.from(client);
const TABLE = 'AfyaQuestData';

const VALID_TYPES = ['module_watched', 'module_quiz_complete', 'lesson_complete', 'itinerary_stop_complete'];

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
    const { type, itemId, date, score } = body;

    if (!type || !itemId) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing required fields: type and itemId' }) };
    }

    if (!VALID_TYPES.includes(type)) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: `Invalid type. Must be one of: ${VALID_TYPES.join(', ')}` }) };
    }

    const timestamp = new Date().toISOString();

    switch (type) {
      case 'module_watched': {
        await ddb.send(new UpdateCommand({
          TableName: TABLE,
          Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#MODULE#${itemId}` },
          UpdateExpression: 'SET #status = :status, updatedAt = :updatedAt',
          ExpressionAttributeNames: { '#status': 'status' },
          ExpressionAttributeValues: {
            ':status': 'in_progress',
            ':updatedAt': timestamp,
          },
        }));
        break;
      }

      case 'module_quiz_complete': {
        const updateExpr = score !== undefined
          ? 'SET #status = :status, updatedAt = :updatedAt, completedAt = :completedAt, score = :score'
          : 'SET #status = :status, updatedAt = :updatedAt, completedAt = :completedAt';
        const exprValues = {
          ':status': 'completed',
          ':updatedAt': timestamp,
          ':completedAt': timestamp,
        };
        if (score !== undefined) {
          exprValues[':score'] = score;
        }

        await ddb.send(new UpdateCommand({
          TableName: TABLE,
          Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#MODULE#${itemId}` },
          UpdateExpression: updateExpr,
          ExpressionAttributeNames: { '#status': 'status' },
          ExpressionAttributeValues: exprValues,
        }));
        break;
      }

      case 'lesson_complete': {
        // Update the assignment status
        await ddb.send(new UpdateCommand({
          TableName: TABLE,
          Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#LESSON#${itemId}` },
          UpdateExpression: 'SET #status = :status, updatedAt = :updatedAt, completedAt = :completedAt',
          ExpressionAttributeNames: { '#status': 'status' },
          ExpressionAttributeValues: {
            ':status': 'completed',
            ':updatedAt': timestamp,
            ':completedAt': timestamp,
          },
        }));

        // Upsert progress record
        await ddb.send(new UpdateCommand({
          TableName: TABLE,
          Key: { PK: `USER#${userId}`, SK: `PROGRESS#${itemId}` },
          UpdateExpression: 'SET completed = :completed, completedAt = :completedAt, updatedAt = :updatedAt',
          ExpressionAttributeValues: {
            ':completed': true,
            ':completedAt': timestamp,
            ':updatedAt': timestamp,
          },
        }));
        break;
      }

      case 'itinerary_stop_complete': {
        if (!date) {
          return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing required field: date (YYYY-MM-DD) for itinerary_stop_complete' }) };
        }

        await ddb.send(new UpdateCommand({
          TableName: TABLE,
          Key: { PK: `USER#${userId}`, SK: `ITINERARY#${date}` },
          UpdateExpression: 'ADD completedStops :stopId SET updatedAt = :updatedAt',
          ExpressionAttributeValues: {
            ':stopId': new Set([itemId]),
            ':updatedAt': timestamp,
          },
        }));
        break;
      }
    }

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ message: 'Progress updated', type, itemId, timestamp }),
    };
  } catch (err) {
    console.error('Error updating progress:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to update progress' }) };
  }
};
