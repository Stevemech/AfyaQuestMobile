const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, ScanCommand, GetCommand } = require('@aws-sdk/lib-dynamodb');

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
    const org = event.queryStringParameters?.organization;
    const chvId = event.queryStringParameters?.chvId;
    const dateFrom = event.queryStringParameters?.dateFrom;
    const dateTo = event.queryStringParameters?.dateTo;

    // Get all reports
    let filterExpr = 'begins_with(SK, :sk)';
    const exprValues = { ':sk': 'REPORT#' };

    if (chvId) {
      filterExpr += ' AND PK = :pk';
      exprValues[':pk'] = `USER#${chvId}`;
    }

    const result = await ddb.send(new ScanCommand({
      TableName: TABLE,
      FilterExpression: filterExpr,
      ExpressionAttributeValues: exprValues,
    }));

    let reports = result.Items || [];

    // Get user profiles for names and org filtering
    const usersResult = await ddb.send(new ScanCommand({
      TableName: TABLE,
      FilterExpression: 'begins_with(PK, :pk) AND SK = :sk',
      ExpressionAttributeValues: { ':pk': 'USER#', ':sk': 'PROFILE' },
    }));
    const usersMap = {};
    for (const u of (usersResult.Items || [])) {
      usersMap[u.PK] = u;
    }

    // Filter by organization
    if (org) {
      reports = reports.filter(r => {
        const user = usersMap[r.PK];
        return user && user.organization === org;
      });
    }

    // Filter by date range
    if (dateFrom) reports = reports.filter(r => r.date >= dateFrom);
    if (dateTo) reports = reports.filter(r => r.date <= dateTo);

    const formatted = reports.map(r => {
      const user = usersMap[r.PK] || {};
      return {
        id: r.id || `${r.PK}-${r.SK}`,
        userId: r.PK.replace('USER#', ''),
        userName: user.name || 'Unknown',
        date: r.date,
        patientsVisited: r.patientsVisited || 0,
        vaccinationsGiven: r.vaccinationsGiven || 0,
        healthEducation: r.healthEducation || '',
        challenges: r.challenges || '',
        notes: r.notes || '',
        createdAt: r.createdAt,
      };
    });

    // Sort by date descending
    formatted.sort((a, b) => b.date.localeCompare(a.date));

    return { statusCode: 200, headers, body: JSON.stringify({ reports: formatted }) };
  } catch (err) {
    console.error('Error fetching reports:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch reports' }) };
  }
};
