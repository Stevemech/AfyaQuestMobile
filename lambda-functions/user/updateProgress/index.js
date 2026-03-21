/**
 * Lambda Function: user-updateProgress
 * Updates user progress for modules, lessons, and itinerary stops
 */
const { randomUUID } = require('crypto');
const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, UpdateCommand, GetCommand, PutCommand } = require('@aws-sdk/lib-dynamodb');

const client = new DynamoDBClient({ region: 'af-south-1' });
const ddb = DynamoDBDocumentClient.from(client);
const TABLE = 'AfyaQuestData';

/** Best-effort admin notification for the CHV's organization */
async function emitOrgNotification(userId, type, meta) {
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
        type,
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

const VALID_TYPES = ['module_watched', 'module_quiz_complete', 'lesson_complete', 'itinerary_stop_complete'];

/** DynamoDB / SDK v3 conditional failure (avoid upsert ghost ASSIGNMENT rows) */
function isConditionalCheckFailed(err) {
  return (
    err?.name === 'ConditionalCheckFailedException'
    || err?.__type === 'com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException'
    || (typeof err?.message === 'string' && err.message.includes('ConditionalCheckFailed'))
  );
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
    const { type, itemId, date, score } = body;

    if (!type || !itemId) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing required fields: type and itemId' }) };
    }

    if (!VALID_TYPES.includes(type)) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: `Invalid type. Must be one of: ${VALID_TYPES.join(', ')}` }) };
    }

    const timestamp = new Date().toISOString();
    /** @type {{ type: string, meta: Record<string, unknown> } | null} */
    let adminNotification = null;

    switch (type) {
      case 'module_watched': {
        try {
          await ddb.send(new UpdateCommand({
            TableName: TABLE,
            Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#MODULE#${itemId}` },
            UpdateExpression: 'SET #status = :status, updatedAt = :updatedAt',
            // Only touch rows created by admin assign (Put); never upsert ghost tasks from video watch
            ConditionExpression: 'attribute_exists(assignedAt)',
            ExpressionAttributeNames: { '#status': 'status' },
            ExpressionAttributeValues: {
              ':status': 'in_progress',
              ':updatedAt': timestamp,
            },
          }));
        } catch (condErr) {
          if (isConditionalCheckFailed(condErr)) break;
          throw condErr;
        }
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

        try {
          await ddb.send(new UpdateCommand({
            TableName: TABLE,
            Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#MODULE#${itemId}` },
            UpdateExpression: updateExpr,
            ConditionExpression: 'attribute_exists(assignedAt)',
            ExpressionAttributeNames: { '#status': 'status' },
            ExpressionAttributeValues: exprValues,
          }));
          adminNotification = {
            type: 'module_quiz_complete',
            meta: score !== undefined ? { itemId, score } : { itemId },
          };
        } catch (condErr) {
          if (isConditionalCheckFailed(condErr)) break;
          throw condErr;
        }
        break;
      }

      case 'lesson_complete': {
        let assignmentOk = false;
        // Update the assignment status (only if admin-assigned; no ghost lesson rows)
        try {
          await ddb.send(new UpdateCommand({
            TableName: TABLE,
            Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#LESSON#${itemId}` },
            UpdateExpression: 'SET #status = :status, updatedAt = :updatedAt, completedAt = :completedAt',
            ConditionExpression: 'attribute_exists(assignedAt)',
            ExpressionAttributeNames: { '#status': 'status' },
            ExpressionAttributeValues: {
              ':status': 'completed',
              ':updatedAt': timestamp,
              ':completedAt': timestamp,
            },
          }));
          assignmentOk = true;
        } catch (condErr) {
          if (!isConditionalCheckFailed(condErr)) throw condErr;
        }

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
        if (assignmentOk) {
          adminNotification = { type: 'lesson_complete', meta: { itemId } };
        }
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
        adminNotification = { type: 'itinerary_stop_complete', meta: { itemId, date } };
        break;
      }
    }

    if (adminNotification) {
      await emitOrgNotification(userId, adminNotification.type, adminNotification.meta);
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
