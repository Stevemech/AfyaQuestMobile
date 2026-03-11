const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, PutCommand, GetCommand, DeleteCommand } = require('@aws-sdk/lib-dynamodb');

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
    const { chvId, type, data, action } = body;

    if (!chvId || !type) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing chvId or type' }) };
    }

    // Handle delete action
    if (action === 'delete') {
      const itemId = data?.moduleId || data?.lessonId;
      if (!itemId) {
        return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing moduleId or lessonId in data' }) };
      }
      const sk = type === 'module'
        ? `ASSIGNMENT#MODULE#${itemId}`
        : type === 'lesson'
          ? `ASSIGNMENT#LESSON#${itemId}`
          : null;
      if (!sk) {
        return { statusCode: 400, headers, body: JSON.stringify({ error: 'Cannot delete this assignment type' }) };
      }
      await ddb.send(new DeleteCommand({
        TableName: TABLE,
        Key: { PK: `USER#${chvId}`, SK: sk },
      }));
      return {
        statusCode: 200,
        headers,
        body: JSON.stringify({ message: `${type} assignment removed from CHV ${chvId}` }),
      };
    }

    // Verify the CHV exists
    const userResult = await ddb.send(new GetCommand({
      TableName: TABLE,
      Key: { PK: `USER#${chvId}`, SK: 'PROFILE' },
    }));

    if (!userResult.Item) {
      return { statusCode: 404, headers, body: JSON.stringify({ error: 'CHV not found' }) };
    }

    const timestamp = new Date().toISOString();

    switch (type) {
      case 'module': {
        const moduleIds = data.moduleIds || (data.moduleId ? [data.moduleId] : []);
        for (const moduleId of moduleIds) {
          await ddb.send(new PutCommand({
            TableName: TABLE,
            Item: {
              PK: `USER#${chvId}`,
              SK: `ASSIGNMENT#MODULE#${moduleId}`,
              type: 'module',
              moduleId: moduleId,
              moduleNumber: data.moduleNumber || null,
              assignedAt: timestamp,
              assignedBy: event.requestContext?.authorizer?.jwt?.claims?.sub || 'admin',
              status: 'assigned',
              dueDate: data.dueDate || null,
            },
          }));
        }
        break;
      }
      case 'lesson': {
        await ddb.send(new PutCommand({
          TableName: TABLE,
          Item: {
            PK: `USER#${chvId}`,
            SK: `ASSIGNMENT#LESSON#${data.lessonId}`,
            type: 'lesson',
            lessonId: data.lessonId,
            assignedAt: timestamp,
            assignedBy: event.requestContext?.authorizer?.jwt?.claims?.sub || 'admin',
            status: 'assigned',
            mandatory: true,
            dueDate: data.dueDate || null,
          },
        }));
        break;
      }
      case 'report': {
        await ddb.send(new PutCommand({
          TableName: TABLE,
          Item: {
            PK: `USER#${chvId}`,
            SK: `ASSIGNMENT#REPORT#${timestamp}`,
            type: 'report',
            assignedAt: timestamp,
            assignedBy: event.requestContext?.authorizer?.jwt?.claims?.sub || 'admin',
            status: 'assigned',
            dueDate: data.dueDate || null,
            reportType: data.reportType || 'daily',
          },
        }));
        break;
      }
      default:
        return { statusCode: 400, headers, body: JSON.stringify({ error: `Unknown assignment type: ${type}` }) };
    }

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ message: `${type} assigned to CHV ${chvId}`, timestamp }),
    };
  } catch (err) {
    console.error('Error assigning task:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to assign task' }) };
  }
};
