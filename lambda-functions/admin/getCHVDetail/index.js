const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, GetCommand, QueryCommand } = require('@aws-sdk/lib-dynamodb');

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
    const chvId = event.pathParameters?.chvId;
    if (!chvId) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing chvId' }) };
    }

    // Get CHV profile
    const profileResult = await ddb.send(new GetCommand({
      TableName: TABLE,
      Key: { PK: `USER#${chvId}`, SK: 'PROFILE' },
    }));

    if (!profileResult.Item) {
      return { statusCode: 404, headers, body: JSON.stringify({ error: 'CHV not found' }) };
    }

    const item = profileResult.Item;

    // Get assigned houses
    const housesResult = await ddb.send(new QueryCommand({
      TableName: TABLE,
      KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
      ExpressionAttributeValues: {
        ':pk': `USER#${chvId}`,
        ':sk': 'HOUSE#',
      },
    }));

    const houses = (housesResult.Items || []).map(h => ({
      id: h.SK.replace('HOUSE#', ''),
      assignedCHV: chvId,
      assignedCHVName: item.name || 'Unknown',
      latitude: h.latitude || 0,
      longitude: h.longitude || 0,
      distance: h.distance || 0,
      visitStatus: h.visitStatus || 'pending',
      priority: h.priority || 'medium',
      daysPending: h.daysPending || null,
      lastVisit: h.lastVisit || null,
    }));

    const chv = {
      id: chvId,
      name: item.name || 'Unknown',
      email: item.email || '',
      phone: item.phone || '',
      clinic: item.location || 'Kibera East',
      assignedHouses: houses.length,
      completedVisits: houses.filter(h => h.visitStatus === 'completed').length,
      pendingVisits: houses.filter(h => h.visitStatus === 'pending').length,
      completionRate: houses.length ? Math.round(houses.filter(h => h.visitStatus === 'completed').length / houses.length * 100) : 0,
      lastActive: item.lastActiveDate || item.updatedAt || 'Unknown',
      isActive: item.isActive !== false,
      status: 'active',
      flags: [],
    };

    return { statusCode: 200, headers, body: JSON.stringify({ chv, houses }) };
  } catch (err) {
    console.error('Error fetching CHV detail:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch CHV detail' }) };
  }
};
