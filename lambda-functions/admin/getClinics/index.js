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

    const result = await ddb.send(new ScanCommand({
      TableName: TABLE,
      FilterExpression: 'begins_with(PK, :pk) AND SK = :sk',
      ExpressionAttributeValues: { ':pk': 'ORG#', ':sk': 'INFO' },
    }));

    const clinics = (result.Items || []).map(item => ({
      id: item.PK.replace('ORG#', ''),
      name: item.name || 'Unknown',
      location: item.location || '',
    }));

    return { statusCode: 200, headers, body: JSON.stringify({ clinics }) };
  } catch (err) {
    console.error('Error fetching clinics:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch clinics' }) };
  }
};
