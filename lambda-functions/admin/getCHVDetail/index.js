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

    // Fetch houses, itineraries, and assignments in parallel
    const [housesResult, itinerariesResult, assignmentsResult] = await Promise.all([
      ddb.send(new QueryCommand({
        TableName: TABLE,
        KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
        ExpressionAttributeValues: { ':pk': `USER#${chvId}`, ':sk': 'HOUSE#' },
      })),
      ddb.send(new QueryCommand({
        TableName: TABLE,
        KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
        ExpressionAttributeValues: { ':pk': `USER#${chvId}`, ':sk': 'ITINERARY#' },
        ScanIndexForward: false,
      })),
      ddb.send(new QueryCommand({
        TableName: TABLE,
        KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
        ExpressionAttributeValues: { ':pk': `USER#${chvId}`, ':sk': 'ASSIGNMENT#' },
      })),
    ]);

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

    const itineraries = (itinerariesResult.Items || []).map(it => {
      const completedStops = it.completedStops
        ? (it.completedStops instanceof Set ? [...it.completedStops] : Array.isArray(it.completedStops) ? it.completedStops : [])
        : [];
      return {
        date: it.date || it.SK.replace('ITINERARY#', ''),
        stops: (it.stops || []).map(s => ({
          order: s.order || 0,
          houseId: s.houseId || '',
          label: s.label || '',
          address: s.address || '',
          description: s.description || '',
          notes: s.notes || '',
          latitude: s.latitude || 0,
          longitude: s.longitude || 0,
          completed: completedStops.includes(s.houseId),
        })),
        completedStops,
        status: it.status || 'active',
        createdAt: it.createdAt || '',
        createdBy: it.createdBy || '',
      };
    });

    const assignments = (assignmentsResult.Items || []).map(a => ({
      type: a.type || 'module',
      moduleId: a.moduleId || null,
      lessonId: a.lessonId || null,
      status: a.status || 'assigned',
      mandatory: a.mandatory || false,
      dueDate: a.dueDate || null,
      assignedAt: a.assignedAt || '',
      assignedBy: a.assignedBy || '',
    }));

    // Calculate status
    let status = 'active';
    if (item.lastActiveDate) {
      const days = Math.floor((Date.now() - new Date(item.lastActiveDate).getTime()) / (1000 * 60 * 60 * 24));
      if (days > 7) status = 'inactive';
      else if (days > 3) status = 'caution';
    }

    const chv = {
      id: chvId,
      name: item.name || 'Unknown',
      email: item.email || '',
      phone: item.phone || '',
      clinic: item.organization || item.location || '',
      organization: item.organization || '',
      role: item.role || 'cha',
      level: item.level || 0,
      totalPoints: item.totalPoints || 0,
      lives: item.lives || 0,
      currentStreak: item.currentStreak || 0,
      assignedHouses: houses.length,
      completedVisits: houses.filter(h => h.visitStatus === 'completed').length,
      pendingVisits: houses.filter(h => h.visitStatus === 'pending' || h.visitStatus === 'overdue').length,
      completionRate: houses.length ? Math.round(houses.filter(h => h.visitStatus === 'completed').length / houses.length * 100) : 0,
      lastActive: item.lastActiveDate || item.updatedAt || '',
      isActive: item.isActive !== false,
      status,
      language: item.language || 'en',
      createdAt: item.createdAt || '',
      flags: [],
    };

    return { statusCode: 200, headers, body: JSON.stringify({ chv, houses, itineraries, assignments }) };
  } catch (err) {
    console.error('Error fetching CHV detail:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch CHV detail' }) };
  }
};
