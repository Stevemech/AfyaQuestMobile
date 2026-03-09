/**
 * Lambda Function: user-getItineraries
 * Returns itineraries assigned to the authenticated user
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

    const date = event.queryStringParameters?.date;

    let keyCondition = 'PK = :pk AND begins_with(SK, :sk)';
    const exprValues = {
      ':pk': `USER#${userId}`,
      ':sk': date ? `ITINERARY#${date}` : 'ITINERARY#',
    };

    const result = await ddb.send(new QueryCommand({
      TableName: TABLE,
      KeyConditionExpression: keyCondition,
      ExpressionAttributeValues: exprValues,
      ScanIndexForward: false,
    }));

    const itineraries = (result.Items || []).map(item => {
      const completedStops = item.completedStops
        ? (item.completedStops instanceof Set ? [...item.completedStops] : Array.isArray(item.completedStops) ? item.completedStops : [])
        : [];
      return {
        date: item.date,
        stops: (item.stops || []).map(s => ({
          ...s,
          completed: completedStops.includes(s.houseId),
        })),
        completedStops,
        status: item.status || 'active',
        createdAt: item.createdAt,
      };
    });

    return { statusCode: 200, headers, body: JSON.stringify({ itineraries }) };
  } catch (err) {
    console.error('Error fetching itineraries:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch itineraries' }) };
  }
};
