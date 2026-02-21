/**
 * Lambda Function: reports-create
 * Creates a daily report for a community health assistant
 */

const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");
const { randomUUID } = require("crypto");

const dynamoClient = new DynamoDBClient({});
const docClient = DynamoDBDocumentClient.from(dynamoClient);

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
