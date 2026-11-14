/**
 * Lambda Function: admin-getOrganizations
 * Returns all organizations (public endpoint for registration dropdown)
 */
const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, ScanCommand } = require('@aws-sdk/lib-dynamodb');

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
    // A scan returns at most 1 MB of the table per request. Follow the pages, or organizations
    // silently drop out of the sign-up list once the table has grown past that.
    const items = [];
    let startKey;
    do {
      const page = await ddb.send(new ScanCommand({
        TableName: TABLE,
        FilterExpression: 'begins_with(PK, :pk) AND SK = :sk',
        ExpressionAttributeValues: {
          ':pk': 'ORG#',
          ':sk': 'INFO',
        },
        ExclusiveStartKey: startKey,
      }));
      items.push(...(page.Items || []));
      startKey = page.LastEvaluatedKey;
    } while (startKey);

    const organizations = items.map(item => ({
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
