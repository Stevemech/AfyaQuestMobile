/**
 * Lambda Function: user-getAssignments
 * Returns modules/lessons/reports assigned to the authenticated user
 */
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
    const userId = event.requestContext?.authorizer?.jwt?.claims?.sub;
    if (!userId) {
      return { statusCode: 401, headers, body: JSON.stringify({ error: 'Unauthorized' }) };
    }

    const result = await ddb.send(new QueryCommand({
      TableName: TABLE,
      KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
      ExpressionAttributeValues: {
        ':pk': `USER#${userId}`,
        ':sk': 'ASSIGNMENT#',
      },
    }));

    const assignments = (result.Items || []).map(item => ({
      type: item.type,
      moduleId: item.moduleId || null,
      lessonId: item.lessonId || null,
      reportType: item.reportType || null,
      status: item.status || 'assigned',
      mandatory: item.mandatory || false,
      dueDate: item.dueDate || null,
      assignedAt: item.assignedAt,
    }));

    return { statusCode: 200, headers, body: JSON.stringify({ assignments }) };
  } catch (err) {
    console.error('Error fetching assignments:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch assignments' }) };
  }
};
