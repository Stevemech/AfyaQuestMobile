/**
 * Lambda Function: admin-getOrganizations
 * Returns all organizations (public endpoint for registration dropdown)
 */
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
    const result = await ddb.send(new ScanCommand({
      TableName: TABLE,
      FilterExpression: 'begins_with(PK, :pk) AND SK = :sk',
      ExpressionAttributeValues: {
        ':pk': 'ORG#',
        ':sk': 'INFO',
      },
    }));

    const organizations = (result.Items || []).map(item => ({
      id: item.PK.replace('ORG#', ''),
      name: item.name,
      location: item.location || '',
    }));

    return { statusCode: 200, headers, body: JSON.stringify({ organizations }) };
  } catch (err) {
    console.error('Error fetching organizations:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to fetch organizations' }) };
  }
};
