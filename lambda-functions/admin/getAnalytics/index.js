const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, ScanCommand } = require('@aws-sdk/lib-dynamodb');

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

    // Get all non-admin users
    const usersResult = await ddb.send(new ScanCommand({
      TableName: TABLE,
      FilterExpression: 'begins_with(PK, :pk) AND SK = :sk AND #role <> :admin',
      ExpressionAttributeNames: { '#role': 'role' },
      ExpressionAttributeValues: { ':pk': 'USER#', ':sk': 'PROFILE', ':admin': 'admin' },
    }));

    let users = usersResult.Items || [];
    if (org) users = users.filter(u => u.organization === org);
    const totalCHVs = users.length || 1;
    const userPKs = new Set(users.map(u => u.PK));

    // Get all reports
    const reportsResult = await ddb.send(new ScanCommand({
      TableName: TABLE,
      FilterExpression: 'begins_with(SK, :sk)',
      ExpressionAttributeValues: { ':sk': 'REPORT#' },
    }));
    const allReports = (reportsResult.Items || []).filter(r => userPKs.has(r.PK));

    // Get all assignments
    const assignResult = await ddb.send(new ScanCommand({
      TableName: TABLE,
      FilterExpression: 'begins_with(SK, :sk)',
      ExpressionAttributeValues: { ':sk': 'ASSIGNMENT#' },
    }));
    const allAssignments = (assignResult.Items || []).filter(
      a => userPKs.has(a.PK) && a.assignedAt
    );

    // Compute stats
    const atRiskCHVs = users.filter(u => {
      if (!u.lastActiveDate) return true;
      const days = Math.floor((Date.now() - new Date(u.lastActiveDate).getTime()) / (1000 * 60 * 60 * 24));
      return days > 5;
    }).length;

    const modulesAssigned = allAssignments.filter(a => a.type === 'module' || a.type === 'lesson').length;

    // Report submission rate (how many users submitted at least 1 report this week)
    const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    const recentReports = allReports.filter(r => (r.date || '') >= weekAgo);
    const chvsWithReports = new Set(recentReports.map(r => r.PK)).size;
    const reportsSubmitted = Math.round((chvsWithReports / totalCHVs) * 100);

    // Average level as a proxy for video completion
    const avgLevel = users.reduce((s, u) => s + (u.level || 0), 0) / totalCHVs;
    const avgVideoCompletion = Math.round(avgLevel * 10); // rough proxy

    const stats = { avgVideoCompletion, atRiskCHVs, modulesAssigned, reportsSubmitted };

    // Per-CHV progress
    const progress = users.map(u => {
      const userId = u.PK.replace('USER#', '');
      const userAssignments = allAssignments.filter(a => a.PK === u.PK);
      const modules = userAssignments.map(a => ({
        id: a.moduleId || a.lessonId || 'unknown',
        type: a.type,
        status: a.status || 'assigned',
        completed: a.status === 'completed',
      }));
      const completed = modules.filter(m => m.completed).length;
      const total = modules.length || 1;
      const overallProgress = Math.round((completed / total) * 100);

      return {
        chvId: userId,
        chvName: u.name || 'Unknown',
        level: u.level || 0,
        totalPoints: u.totalPoints || 0,
        lives: u.lives ?? 0,
        currentStreak: u.currentStreak ?? 0,
        modules,
        overallProgress,
      };
    });

    // Weekly reports summary
    const weeklyReports = users.map(u => {
      const userId = u.PK.replace('USER#', '');
      const userReports = recentReports.filter(r => r.PK === u.PK);
      const totalPatients = userReports.reduce((s, r) => s + (r.patientsVisited || 0), 0);
      const hasHighRisk = userReports.some(r => r.challenges && r.challenges.length > 0);

      return {
        chvId: userId,
        chvName: u.name || 'Unknown',
        week: weekAgo + ' to ' + new Date().toISOString().split('T')[0],
        reportsCount: userReports.length,
        totalPatients,
        highRisk: hasHighRisk ? userReports.find(r => r.challenges)?.challenges || '' : '',
        submitted: userReports.length > 0,
      };
    });

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ stats, progress, reports: weeklyReports }),
    };
  } catch (err) {
    console.error('Error fetching analytics:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch analytics' }) };
  }
};
