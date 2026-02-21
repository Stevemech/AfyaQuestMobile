/**
 * One-time script: Reset all existing user accounts to clean defaults.
 *
 * Run with:
 *   AWS_PROFILE=afyaquest AWS_REGION=af-south-1 node reset-accounts.js
 */

const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, ScanCommand, UpdateCommand } = require("@aws-sdk/lib-dynamodb");

const TABLE_NAME = "AfyaQuestData";

const client = new DynamoDBClient({ region: process.env.AWS_REGION || "af-south-1" });
const docClient = DynamoDBDocumentClient.from(client);

async function resetAllAccounts() {
    let lastEvaluatedKey = undefined;
    let totalUpdated = 0;

    console.log("Scanning for USER#*/PROFILE records...");

    do {
        const scanResult = await docClient.send(new ScanCommand({
            TableName: TABLE_NAME,
            FilterExpression: "begins_with(PK, :pk) AND SK = :sk",
            ExpressionAttributeValues: {
                ":pk": "USER#",
                ":sk": "PROFILE"
            },
            ExclusiveStartKey: lastEvaluatedKey
        }));

        const items = scanResult.Items || [];
        console.log(`Found ${items.length} profiles in this batch`);

        for (const item of items) {
            console.log(`Resetting ${item.PK} (${item.name || item.email || 'unknown'})...`);

            await docClient.send(new UpdateCommand({
                TableName: TABLE_NAME,
                Key: {
                    PK: item.PK,
                    SK: item.SK
                },
                UpdateExpression: "SET lives = :lives, totalPoints = :totalPoints, currentStreak = :streak, #level = :level, #rank = :rank, dailyXP = :dailyXP, weeklyXP = :weeklyXP, updatedAt = :updatedAt",
                ExpressionAttributeNames: {
                    "#level": "level",
                    "#rank": "rank"
                },
                ExpressionAttributeValues: {
                    ":lives": 10,
                    ":totalPoints": 0,
                    ":streak": 0,
                    ":level": 0,
                    ":rank": "Beginner",
                    ":dailyXP": 0,
                    ":weeklyXP": 0,
                    ":updatedAt": new Date().toISOString()
                }
            }));

            totalUpdated++;
        }

        lastEvaluatedKey = scanResult.LastEvaluatedKey;
    } while (lastEvaluatedKey);

    console.log(`\nDone! Reset ${totalUpdated} account(s).`);
}

resetAllAccounts().catch(err => {
    console.error("Error:", err);
    process.exit(1);
});
