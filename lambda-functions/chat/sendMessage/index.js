/**
 * Lambda Function: chat-sendMessage
 * Sends message to Amazon Bedrock (Claude) and stores conversation
 */

const { BedrockRuntimeClient, InvokeModelCommand } = require("@aws-sdk/client-bedrock-runtime");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand, QueryCommand } = require("@aws-sdk/lib-dynamodb");
const { randomUUID } = require("crypto");

const bedrockClient = new BedrockRuntimeClient({ region: process.env.AWS_REGION });
const dynamoClient = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(dynamoClient);

const SYSTEM_PROMPT = `You are Fred, a friendly and knowledgeable health assistant for Community Health Assistants (CHAs) in East Africa. Your role is to:

1. Provide accurate, evidence-based health information
2. Offer practical advice for community health work
3. Give study tips and educational support
4. Encourage and motivate CHAs in their important work
5. Be culturally sensitive to East African contexts
6. Explain medical concepts in simple, clear language
7. Focus on preventive care and health education

Guidelines:
- Keep responses concise and practical (2-3 paragraphs maximum)
- Use simple language suitable for non-native English speakers
- Acknowledge when you don't know something
- Never provide emergency medical advice - always recommend seeking professional help for emergencies
- Be supportive and encouraging
- Reference local health practices when appropriate

Remember: CHAs are frontline health workers serving their communities. Your guidance helps them help others.`;

exports.handler = async (event) => {
    console.log('Chat message request:', JSON.stringify(event, null, 2));

    try {
        const body = JSON.parse(event.body);
        const { message } = body;

        // Extract user ID from authorizer
        const userId = event.requestContext?.authorizer?.jwt?.claims?.sub
            || event.requestContext?.authorizer?.claims?.sub;

        if (!userId) {
            return {
                statusCode: 401,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ error: 'Unauthorized' })
            };
        }

        if (!message) {
            return {
                statusCode: 400,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ error: 'Message is required' })
            };
        }

        // Get recent conversation history (last 10 messages)
        const queryCommand = new QueryCommand({
            TableName: process.env.DYNAMODB_TABLE,
            KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
            ExpressionAttributeValues: {
                ':pk': `USER#${userId}`,
                ':sk': 'CHAT#'
            },
            ScanIndexForward: false,
            Limit: 10
        });

        const historyResult = await docClient.send(queryCommand);
        const history = (historyResult.Items || []).reverse();

        // Build conversation for Claude
        const messages = [];
        for (const item of history) {
            messages.push({ role: 'user', content: item.message });
            if (item.response) {
                messages.push({ role: 'assistant', content: item.response });
            }
        }
        messages.push({ role: 'user', content: message });

        // Call Bedrock
        const bedrockInput = {
            modelId: process.env.BEDROCK_MODEL_ID || 'anthropic.claude-3-5-sonnet-20241022-v2:0',
            contentType: 'application/json',
            accept: 'application/json',
            body: JSON.stringify({
                anthropic_version: 'bedrock-2023-05-31',
                max_tokens: 500,
                system: SYSTEM_PROMPT,
                messages: messages
            })
        };

        const command = new InvokeModelCommand(bedrockInput);
        const response = await bedrockClient.send(command);
        const responseBody = JSON.parse(new TextDecoder().decode(response.body));

        const assistantMessage = responseBody.content[0].text;

        // Save to DynamoDB
        const timestamp = new Date().toISOString();
        const messageId = randomUUID();

        const chatItem = {
            PK: `USER#${userId}`,
            SK: `CHAT#${timestamp}#${messageId}`,
            id: messageId,
            userId,
            message,
            response: assistantMessage,
            role: 'assistant',
            createdAt: timestamp
        };

        const putCommand = new PutCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Item: chatItem
        });

        await docClient.send(putCommand);

        return {
            statusCode: 200,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({
                response: assistantMessage,
                messageId,
                timestamp
            })
        };

    } catch (error) {
        console.error('Chat error:', error);

        return {
            statusCode: 500,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({
                error: 'Failed to process chat message',
                details: error.message
            })
        };
    }
};
