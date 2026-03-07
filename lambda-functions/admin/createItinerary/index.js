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
    const { chvId, date, stops } = body;

    if (!chvId || !date || !stops || !Array.isArray(stops)) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing chvId, date, or stops' }) };
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

    // Store itinerary
    await ddb.send(new PutCommand({
      TableName: TABLE,
      Item: {
        PK: `USER#${chvId}`,
        SK: `ITINERARY#${date}`,
        date,
        stops: stops.map((s, i) => ({
          order: s.order || i + 1,
          houseId: s.houseId || '',
          label: s.label || `Stop ${i + 1}`,
          address: s.address || '',
          description: s.description || '',
          latitude: s.latitude || 0,
          longitude: s.longitude || 0,
        })),
        createdAt: timestamp,
        createdBy: event.requestContext?.authorizer?.jwt?.claims?.sub || 'admin',
        status: 'active',
      },
    }));

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ message: `Itinerary created for ${date}`, chvId, date }),
    };
  } catch (err) {
    console.error('Error creating itinerary:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to create itinerary' }) };
  }
};
