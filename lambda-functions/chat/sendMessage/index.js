/**
 * Lambda Function: afyaquest-chat-sendMessage
 * Sends a message to Groq and stores the conversation.
 *
 * Groq exposes an OpenAI-compatible chat/completions endpoint, so this uses
 * plain fetch (built into the nodejs20.x runtime) rather than an SDK -- it
 * keeps the deployment package small and adds no dependency to audit.
 */

const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, PutCommand, QueryCommand } = require("@aws-sdk/lib-dynamodb");
const { randomUUID } = require("crypto");

const dynamoClient = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(dynamoClient);

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
const GROQ_MODEL = process.env.GROQ_MODEL || "openai/gpt-oss-20b";
const MAX_TOKENS = Number(process.env.GROQ_MAX_TOKENS || 800);
// gpt-oss models reason before answering and bill that against max_tokens. At
// the default effort a 500-token budget went ~240 tokens on reasoning and the
// answer was cut off mid-sentence; "low" drops that to single digits. Set this
// to "" if GROQ_MODEL is ever switched to a model that rejects the field.
const REASONING_EFFORT = process.env.GROQ_REASONING_EFFORT ?? "low";
// Leave headroom under the Lambda's 30s timeout so a slow upstream surfaces as
// our own error rather than an opaque Lambda timeout with no log line.
const REQUEST_TIMEOUT_MS = Number(process.env.GROQ_TIMEOUT_MS || 20000);

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

const HEADERS = {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
};

async function callGroq(messages) {
    const apiKey = process.env.GROQ_API_KEY;
    if (!apiKey) {
        const err = new Error("GROQ_API_KEY is not configured on this function");
        err.statusCode = 503;
        throw err;
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    let res;
    try {
        res = await fetch(GROQ_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${apiKey}`,
            },
            body: JSON.stringify({
                model: GROQ_MODEL,
                max_tokens: MAX_TOKENS,
                ...(REASONING_EFFORT ? { reasoning_effort: REASONING_EFFORT } : {}),
                messages: [{ role: "system", content: SYSTEM_PROMPT }, ...messages],
            }),
            signal: controller.signal,
        });
    } catch (e) {
        const err = new Error(
            e.name === "AbortError"
                ? `Groq request timed out after ${REQUEST_TIMEOUT_MS}ms`
                : `Could not reach Groq: ${e.message}`
        );
        err.statusCode = 504;
        throw err;
    } finally {
        clearTimeout(timer);
    }

    const raw = await res.text();
    if (!res.ok) {
        // Surface Groq's own message (bad key, unknown model, rate limit) instead
        // of a generic 500 -- these are the failures worth seeing in the logs.
        let detail = raw.slice(0, 400);
        try { detail = JSON.parse(raw)?.error?.message || detail; } catch { /* keep raw */ }
        const err = new Error(`Groq ${res.status}: ${detail}`);
        err.statusCode = res.status === 429 ? 429 : 502;
        throw err;
    }

    let parsed;
    try {
        parsed = JSON.parse(raw);
    } catch {
        const err = new Error("Groq returned a non-JSON response");
        err.statusCode = 502;
        throw err;
    }

    const choice = parsed?.choices?.[0];
    const text = choice?.message?.content;
    if (!text) {
        const err = new Error("Groq returned no message content");
        err.statusCode = 502;
        throw err;
    }
    // Worth seeing in the logs: a truncated answer still returns 200, so this is
    // the only signal that MAX_TOKENS needs raising.
    if (choice.finish_reason === "length") {
        console.warn(`Response truncated at ${MAX_TOKENS} tokens (model=${GROQ_MODEL})`);
    }
    return text;
}

exports.handler = async (event) => {
    try {
        const userId = event.requestContext?.authorizer?.jwt?.claims?.sub
            || event.requestContext?.authorizer?.claims?.sub;
        if (!userId) {
            return { statusCode: 401, headers: HEADERS, body: JSON.stringify({ error: "Unauthorized" }) };
        }

        const { message } = JSON.parse(event.body || "{}");
        if (!message) {
            return { statusCode: 400, headers: HEADERS, body: JSON.stringify({ error: "Message is required" }) };
        }

        // Recent turns, oldest-first, so Fred keeps context across the conversation.
        const historyResult = await docClient.send(new QueryCommand({
            TableName: process.env.DYNAMODB_TABLE,
            KeyConditionExpression: "PK = :pk AND begins_with(SK, :sk)",
            ExpressionAttributeValues: { ":pk": `USER#${userId}`, ":sk": "CHAT#" },
            ScanIndexForward: false,
            Limit: 10,
        }));

        const messages = [];
        for (const item of (historyResult.Items || []).reverse()) {
            messages.push({ role: "user", content: item.message });
            if (item.response) messages.push({ role: "assistant", content: item.response });
        }
        messages.push({ role: "user", content: message });

        const assistantMessage = await callGroq(messages);

        const timestamp = new Date().toISOString();
        const messageId = randomUUID();
        await docClient.send(new PutCommand({
            TableName: process.env.DYNAMODB_TABLE,
            Item: {
                PK: `USER#${userId}`,
                SK: `CHAT#${timestamp}#${messageId}`,
                id: messageId,
                userId,
                message,
                response: assistantMessage,
                role: "assistant",
                model: GROQ_MODEL,
                createdAt: timestamp,
            },
        }));

        return {
            statusCode: 200,
            headers: HEADERS,
            body: JSON.stringify({ response: assistantMessage, messageId, timestamp }),
        };
    } catch (error) {
        console.error("Chat error:", error);
        return {
            statusCode: error.statusCode || 500,
            headers: HEADERS,
            body: JSON.stringify({ error: "Failed to process chat message", details: error.message }),
        };
    }
};
