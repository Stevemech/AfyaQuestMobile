const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, PutCommand, GetCommand } = require('@aws-sdk/lib-dynamodb');

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
    const { chvId, type, data } = body;

    if (!chvId || !type) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing chvId or type' }) };
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
        await ddb.send(new PutCommand({
          TableName: TABLE,
          Item: {
            PK: `USER#${chvId}`,
            SK: `ASSIGNMENT#MODULE#${data.moduleId}`,
            type: 'module',
            moduleId: data.moduleId,
            assignedAt: timestamp,
            assignedBy: event.requestContext?.authorizer?.jwt?.claims?.sub || 'admin',
            status: 'assigned',
            dueDate: data.dueDate || null,
          },
        }));
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
