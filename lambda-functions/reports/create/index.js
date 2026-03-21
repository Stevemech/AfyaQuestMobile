/**
 * Lambda Function: reports-create
 * Creates a daily report for a community health assistant
 */

const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand, GetCommand, QueryCommand, UpdateCommand } = require("@aws-sdk/lib-dynamodb");
const { randomUUID } = require("crypto");

const dynamoClient = new DynamoDBClient({});
const docClient = DynamoDBDocumentClient.from(dynamoClient);

async function emitOrgNotification(tableName, userId, notif) {
    try {
        const prof = await docClient.send(new GetCommand({
            TableName: tableName,
            Key: { PK: `USER#${userId}`, SK: 'PROFILE' },
        }));
        const org = prof.Item?.organization;
        if (!org) return;
        const ts = new Date().toISOString();
        const id = randomUUID();
        const sk = `NOTIF#${ts}#${id}`;
        await docClient.send(new PutCommand({
            TableName: tableName,
            Item: {
                PK: `ORG#${org}`,
                SK: sk,
                id,
                type: notif.type,
                meta: notif.meta || {},
                chvId: userId,
                chvName: prof.Item?.name || 'Unknown',
                createdAt: ts,
                read: false,
            },
        }));
    } catch (e) {
        console.warn('emitOrgNotification failed (non-fatal):', e);
    }
}

exports.handler = async (event) => {
    console.log('Create report request:', JSON.stringify(event, null, 2));

    const headers = {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
    };

    try {
        // Extract user ID from JWT authorizer
        const userId = event.requestContext?.authorizer?.jwt?.claims?.sub;

        if (!userId) {
            return {
                statusCode: 401,
                headers,
                body: JSON.stringify({ error: 'Unauthorized' })
            };
        }

        const body = JSON.parse(event.body);
        const { date, patientsVisited, vaccinationsGiven, healthEducation, challenges, notes } = body;

        if (!date || patientsVisited === undefined || vaccinationsGiven === undefined || !healthEducation) {
            return {
                statusCode: 400,
                headers,
                body: JSON.stringify({ error: 'date, patientsVisited, vaccinationsGiven, and healthEducation are required' })
            };
        }

        const timestamp = new Date().toISOString();
        const reportId = randomUUID();

        const reportItem = {
            PK: `USER#${userId}`,
            SK: `REPORT#${date}`,
            id: reportId,
            userId,
            date,
            patientsVisited: Number(patientsVisited),
            vaccinationsGiven: Number(vaccinationsGiven),
            healthEducation: healthEducation || '',
            challenges: challenges || '',
            notes: notes || '',
            createdAt: timestamp,
            updatedAt: timestamp
        };

        await docClient.send(new PutCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Item: reportItem
        }));

        await emitOrgNotification(process.env.DYNAMODB_TABLE, userId, {
            type: 'daily_report_submitted',
            meta: {
                date,
                patientsVisited: Number(patientsVisited),
                vaccinationsGiven: Number(vaccinationsGiven),
            },
        });

        // Mark any pending report assignments as completed
        try {
            const assignResult = await docClient.send(new QueryCommand({
                TableName: process.env.DYNAMODB_TABLE,
                KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
                ExpressionAttributeValues: {
                    ':pk': `USER#${userId}`,
                    ':sk': 'ASSIGNMENT#REPORT#',
                },
            }));

            const pendingReports = (assignResult.Items || []).filter(
                item => item.status !== 'completed'
            );

            for (const item of pendingReports) {
                await docClient.send(new UpdateCommand({
                    TableName: process.env.DYNAMODB_TABLE,
                    Key: { PK: item.PK, SK: item.SK },
                    UpdateExpression: 'SET #status = :status, completedAt = :ts, updatedAt = :ts',
                    ExpressionAttributeNames: { '#status': 'status' },
                    ExpressionAttributeValues: {
                        ':status': 'completed',
                        ':ts': timestamp,
                    },
                }));
            }
        } catch (assignErr) {
            console.warn('Non-critical: failed to update report assignments:', assignErr);
        }

        return {
            statusCode: 200,
            headers,
            body: JSON.stringify({
                message: 'Report created successfully',
                reportId,
                date
            })
        };

    } catch (error) {
        console.error('Create report error:', error);

        return {
            statusCode: 500,
            headers,
            body: JSON.stringify({
                error: 'Failed to create report',
                details: error.message
            })
        };
    }
};
