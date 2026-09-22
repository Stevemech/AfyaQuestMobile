const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, ScanCommand } = require('@aws-sdk/lib-dynamodb');
const { requireAdmin } = require('./authz');

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const ddb = DynamoDBDocumentClient.from(client);
const TABLE = 'AfyaQuestData';

exports.handler = async (event) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type,Authorization',
    'Content-Type': 'application/json',
  };

  try {
    // Admin-only: the JWT authorizer proves identity, not role.
    const auth = await requireAdmin(event);
    if (auth.denied) return auth.denied;

    const org = auth.organization;

    // Scan for all user profiles (non-admin)
    const result = await ddb.send(new ScanCommand({
      TableName: TABLE,
      FilterExpression: 'begins_with(PK, :pk) AND SK = :sk AND #role <> :admin',
      ExpressionAttributeNames: { '#role': 'role' },
      ExpressionAttributeValues: {
        ':pk': 'USER#',
        ':sk': 'PROFILE',
        ':admin': 'admin',
      },
    }));

    let users = result.Items || [];

    // Filter by organization if specified
    if (org) {
      users = users.filter(u => u.organization === org);
    }

    // For each user, count their reports and assignments
    const chvs = users.map(item => {
      const userId = item.PK.replace('USER#', '');
      const daysSinceActive = item.lastActiveDate
        ? Math.floor((Date.now() - new Date(item.lastActiveDate).getTime()) / (1000 * 60 * 60 * 24))
        : 999;

      let status = 'active';
      if (daysSinceActive > 7) status = 'inactive';
      else if (daysSinceActive > 3) status = 'caution';

      return {
        id: userId,
        name: item.name || 'Unknown',
        email: item.email || '',
        phone: item.phone || '',
        clinic: item.organization || '',
        organization: item.organization || '',
        role: item.role || 'cha',
        level: item.level || 0,
        totalPoints: item.totalPoints || 0,
        lives: item.lives || 10,
        currentStreak: item.currentStreak || 0,
        lastActive: item.lastActiveDate || 'Never',
        isActive: item.isActive !== false,
        status,
        manualStatus: item.manualStatus || null,
        lastClockIn: item.lastClockIn || null,
        lastClockOut: item.lastClockOut || null,
        language: item.language || 'en',
        createdAt: item.createdAt,
      };
    });

    return { statusCode: 200, headers, body: JSON.stringify({ chvs }) };
  } catch (err) {
    console.error('Error fetching CHVs:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch CHVs' }) };
  }
};
