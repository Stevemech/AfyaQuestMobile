/**
 * Lambda Function: questions-getDaily
 * Returns 3 daily questions based on current date
 */

const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, QueryCommand } = require("@aws-sdk/lib-dynamodb");

const dynamoClient = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(dynamoClient);

exports.handler = async (event) => {
    console.log('Get daily questions request:', JSON.stringify(event, null, 2));

    try {
        // Get current date in YYYY-MM-DD format
        const today = new Date().toISOString().split('T')[0];

        // Query questions for today
        const queryCommand = new QueryCommand({
            TableName: process.env.DYNAMODB_TABLE,
            KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
            ExpressionAttributeValues: {
                ':pk': `QUESTION#${today}`,
                ':sk': 'Q#'
            }
        });

        const result = await docClient.send(queryCommand);

        // If no questions for today, generate sample questions
        if (!result.Items || result.Items.length === 0) {
            const sampleQuestions = generateSampleQuestions(today);

            return {
                statusCode: 200,
                headers: {
                    'Content-Type': 'application/json',
                    'Access-Control-Allow-Origin': '*'
                },
                body: JSON.stringify({
                    date: today,
                    questions: sampleQuestions,
                    note: 'Sample questions - no questions configured for today'
                })
            };
        }

        return {
            statusCode: 200,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({
                date: today,
                questions: result.Items
            })
        };

    } catch (error) {
        console.error('Get daily questions error:', error);

        return {
            statusCode: 500,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({
                error: 'Failed to retrieve daily questions',
                details: error.message
            })
        };
    }
};

function generateSampleQuestions(date) {
    return [
        {
            id: `q1-${date}`,
            date,
            question: "What is the recommended frequency for handwashing in healthcare settings?",
            options: [
                "Once at the start of shift",
                "Before and after patient contact",
                "Only when hands look dirty",
                "Every 2 hours"
            ],
            correctAnswer: 1,
            explanation: "Healthcare workers should wash hands before and after every patient contact to prevent cross-contamination and protect both patients and themselves.",
            category: "hygiene",
            points: 30,
            difficulty: "easy",
            order: 1
        },
        {
            id: `q2-${date}`,
            date,
            question: "At what age should a child receive the first dose of measles vaccine?",
            options: [
                "At birth",
                "6 months",
                "9 months",
                "12 months"
            ],
            correctAnswer: 2,
            explanation: "The first dose of measles vaccine is typically given at 9 months of age in most African countries, with a second dose at 15-18 months.",
            category: "prevention",
            points: 30,
            difficulty: "medium",
            order: 2
        },
        {
            id: `q3-${date}`,
            date,
            question: "Which of the following is a danger sign in a newborn requiring immediate referral?",
            options: [
                "Sleeping for 2-3 hours between feeds",
                "Difficulty breathing or fast breathing",
                "Crying when hungry",
                "Passing urine 6-8 times daily"
            ],
            correctAnswer: 1,
            explanation: "Difficulty breathing or fast breathing in a newborn is a danger sign indicating possible serious infection or respiratory distress, requiring immediate medical attention.",
            category: "maternal",
            points: 30,
            difficulty: "hard",
            order: 3
        }
    ];
}
