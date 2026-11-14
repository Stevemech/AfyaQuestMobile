/**
 * Lambda Function: user-updateProgress
 * Updates user progress for modules, lessons, and itinerary stops
 */
const { randomUUID } = require('crypto');
const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const { DynamoDBDocumentClient, UpdateCommand, GetCommand, PutCommand, QueryCommand } = require('@aws-sdk/lib-dynamodb');

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const ddb = DynamoDBDocumentClient.from(client);
const TABLE = 'AfyaQuestData';

// ---------------------------------------------------------------------------
// Account-level learning state ("state_sync")
//
// The app earns XP and records progress offline, then uploads a snapshot of its
// state. The snapshot is MERGED into what the server already holds, so a second
// device (or a re-install) can never wipe progress: XP only goes up, id sets are
// unioned, and the streak / daily-questions entries with the later date win.
// The merged result is returned so the app can adopt it, and the headline
// numbers are mirrored onto PROFILE for the admin portal and the login response.
// ---------------------------------------------------------------------------

/** Progressive level thresholds: must match XpManager in the mobile app. */
const LEVEL_THRESHOLDS = [0, 100, 250, 500, 850, 1300, 1900, 2650, 3550, 4600];
const XP_PER_LEVEL_AFTER_TABLE = 1200;
const MAX_LIVES = 10;
const MAX_IDS = 500;

function calculateLevel(totalXP) {
  for (let i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
    if (totalXP >= LEVEL_THRESHOLDS[i]) {
      if (i === LEVEL_THRESHOLDS.length - 1 && totalXP > LEVEL_THRESHOLDS[i]) {
        return i + Math.floor((totalXP - LEVEL_THRESHOLDS[i]) / XP_PER_LEVEL_AFTER_TABLE);
      }
      return i;
    }
  }
  return 0;
}

function calculateRank(level) {
  if (level < 2) return 'Beginner';
  if (level < 5) return 'Novice';
  if (level < 10) return 'Apprentice';
  if (level < 15) return 'Practitioner';
  if (level < 20) return 'Expert';
  if (level < 30) return 'Master';
  return 'Grand Master';
}

const wholeNumber = (value, fallback = 0) => {
  const n = Number(value);
  return Number.isFinite(n) ? Math.max(0, Math.floor(n)) : fallback;
};
const isIsoDate = (value) => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
const idList = (value) => (Array.isArray(value)
  ? value.filter((id) => typeof id === 'string' && id.length > 0 && id.length <= 80)
  : []);
const unionIds = (a, b) => [...new Set([...idList(a), ...idList(b)])].sort().slice(0, MAX_IDS);

/** Pure merge of the stored state, the profile totals and an uploaded snapshot. */
function mergeState(existing, profile, incoming) {
  const totalPoints = Math.max(
    wholeNumber(existing.totalPoints),
    wholeNumber(profile.totalPoints),
    wholeNumber(incoming.totalPoints)
  );

  // Streak: the entry with the later activity date wins; same day keeps the longer streak.
  const oldStreak = { streak: wholeNumber(existing.currentStreak), date: isIsoDate(existing.lastStreakDate) ? existing.lastStreakDate : '' };
  const newStreak = { streak: wholeNumber(incoming.currentStreak), date: isIsoDate(incoming.lastStreakDate) ? incoming.lastStreakDate : '' };
  let streak = oldStreak;
  if (newStreak.date > oldStreak.date) streak = newStreak;
  else if (newStreak.date === oldStreak.date) streak = { date: oldStreak.date, streak: Math.max(oldStreak.streak, newStreak.streak) };

  // Daily questions: one session per day; the later day wins.
  const oldDaily = existing.dailyQuestions && isIsoDate(existing.dailyQuestions.date) ? existing.dailyQuestions : null;
  const newDaily = incoming.dailyQuestions && isIsoDate(incoming.dailyQuestions.date)
    ? {
      date: incoming.dailyQuestions.date,
      correct: wholeNumber(incoming.dailyQuestions.correct),
      total: wholeNumber(incoming.dailyQuestions.total),
      xp: wholeNumber(incoming.dailyQuestions.xp),
    }
    : null;
  let dailyQuestions = oldDaily;
  if (newDaily && (!oldDaily || newDaily.date >= oldDaily.date)) dailyQuestions = newDaily;

  // Lives move both ways, so the device that is reporting now is the truth.
  const fallbackLives = existing.lives !== undefined ? existing.lives : (profile.lives !== undefined ? profile.lives : MAX_LIVES);
  const lives = Math.min(MAX_LIVES, wholeNumber(incoming.lives !== undefined ? incoming.lives : fallbackLives, MAX_LIVES));

  const level = calculateLevel(totalPoints);
  return {
    totalPoints,
    level,
    rank: calculateRank(level),
    lives,
    currentStreak: streak.streak,
    lastStreakDate: streak.date,
    // Best streak ever reached: never drops, so achievements can rely on it.
    longestStreak: Math.max(wholeNumber(existing.longestStreak), wholeNumber(incoming.longestStreak), streak.streak),
    watchedVideos: unionIds(existing.watchedVideos, incoming.watchedVideos),
    completedQuizzes: unionIds(existing.completedQuizzes, incoming.completedQuizzes),
    completedLessons: unionIds(existing.completedLessons, incoming.completedLessons),
    reportDates: unionIds(existing.reportDates, incoming.reportDates),
    dailyQuestions,
  };
}

async function handleStateSync(userId, incoming, headers) {
  if (!incoming || typeof incoming !== 'object' || Array.isArray(incoming)) {
    return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing required field: state' }) };
  }
  const [stateRes, profileRes] = await Promise.all([
    ddb.send(new GetCommand({ TableName: TABLE, Key: { PK: `USER#${userId}`, SK: 'STATE' } })),
    ddb.send(new GetCommand({ TableName: TABLE, Key: { PK: `USER#${userId}`, SK: 'PROFILE' } })),
  ]);
  if (!profileRes.Item) {
    return { statusCode: 404, headers, body: JSON.stringify({ error: 'User not found' }) };
  }

  const merged = mergeState(stateRes.Item || {}, profileRes.Item, incoming);
  const timestamp = new Date().toISOString();

  await ddb.send(new PutCommand({
    TableName: TABLE,
    Item: { PK: `USER#${userId}`, SK: 'STATE', ...merged, updatedAt: timestamp },
  }));
  await ddb.send(new UpdateCommand({
    TableName: TABLE,
    Key: { PK: `USER#${userId}`, SK: 'PROFILE' },
    UpdateExpression: 'SET totalPoints = :xp, #level = :level, #rank = :rank, lives = :lives, currentStreak = :streak, updatedAt = :ts, lastActiveDate = :ts',
    ExpressionAttributeNames: { '#level': 'level', '#rank': 'rank' },
    ExpressionAttributeValues: {
      ':xp': merged.totalPoints,
      ':level': merged.level,
      ':rank': merged.rank,
      ':lives': merged.lives,
      ':streak': merged.currentStreak,
      ':ts': timestamp,
    },
  }));

  // A lesson or quiz finished without a connection reaches the server only inside this snapshot:
  // the direct lesson_complete / module_quiz_complete call never arrived. The supervisor's
  // assignment still has to be closed, or it stays "assigned" for ever.
  try {
    await closeCompletedAssignments(userId, merged, timestamp);
  } catch (e) {
    console.warn('closeCompletedAssignments failed (non-fatal):', e);
  }

  return {
    statusCode: 200,
    headers,
    body: JSON.stringify({ message: 'State synced', state: { ...merged, updatedAt: timestamp } }),
  };
}

/**
 * Which of the user's assignment rows the synced state proves are done.
 * Only rows an admin created (they carry assignedAt) that are not completed yet.
 * A lesson assignment is done when its lesson is completed; a module/video assignment when the
 * quiz of that video is completed (the same rule the app uses to tick the task).
 */
function assignmentsToClose(assignments, state) {
  const lessons = new Set(Array.isArray(state?.completedLessons) ? state.completedLessons : []);
  const quizzes = new Set(Array.isArray(state?.completedQuizzes) ? state.completedQuizzes : []);
  return (assignments || []).filter((a) => {
    if (!a || !a.assignedAt || a.status === 'completed') return false;
    if (a.type === 'lesson') return !!a.lessonId && lessons.has(a.lessonId);
    if (a.type === 'module' || a.type === 'video') return !!a.moduleId && quizzes.has(a.moduleId);
    return false;
  });
}

async function closeCompletedAssignments(userId, state, timestamp) {
  const res = await ddb.send(new QueryCommand({
    TableName: TABLE,
    KeyConditionExpression: 'PK = :pk AND begins_with(SK, :sk)',
    ExpressionAttributeValues: { ':pk': `USER#${userId}`, ':sk': 'ASSIGNMENT#' },
  }));

  for (const assignment of assignmentsToClose(res.Items, state)) {
    try {
      await ddb.send(new UpdateCommand({
        TableName: TABLE,
        Key: { PK: assignment.PK, SK: assignment.SK },
        UpdateExpression: 'SET #status = :completed, updatedAt = :ts, completedAt = :ts',
        // Not completed in the meantime by the direct call: one notification per completion.
        ConditionExpression: 'attribute_exists(assignedAt) AND #status <> :completed',
        ExpressionAttributeNames: { '#status': 'status' },
        ExpressionAttributeValues: { ':completed': 'completed', ':ts': timestamp },
      }));
    } catch (condErr) {
      if (isConditionalCheckFailed(condErr)) continue;
      throw condErr;
    }
    const isLesson = assignment.type === 'lesson';
    await emitOrgNotification(
      userId,
      isLesson ? 'lesson_complete' : 'module_quiz_complete',
      { itemId: isLesson ? assignment.lessonId : assignment.moduleId }
    );
  }
}

/** Best-effort admin notification for the CHV's organization */
async function emitOrgNotification(userId, type, meta) {
  try {
    const prof = await ddb.send(new GetCommand({
      TableName: TABLE,
      Key: { PK: `USER#${userId}`, SK: 'PROFILE' },
    }));
    const org = prof.Item?.organization;
    if (!org) return;
    const ts = new Date().toISOString();
    const id = randomUUID();
    const sk = `NOTIF#${ts}#${id}`;
    await ddb.send(new PutCommand({
      TableName: TABLE,
      Item: {
        PK: `ORG#${org}`,
        SK: sk,
        id,
        type,
        meta: meta || {},
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

const VALID_TYPES = ['module_watched', 'module_quiz_complete', 'lesson_complete', 'itinerary_stop_complete'];

/** DynamoDB / SDK v3 conditional failure (avoid upsert ghost ASSIGNMENT rows) */
function isConditionalCheckFailed(err) {
  return (
    err?.name === 'ConditionalCheckFailedException'
    || err?.__type === 'com.amazonaws.dynamodb.v20120810#ConditionalCheckFailedException'
    || (typeof err?.message === 'string' && err.message.includes('ConditionalCheckFailed'))
  );
}

exports.handler = async (event) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type,Authorization',
    'Content-Type': 'application/json',
  };

  try {
    const userId = event.requestContext?.authorizer?.jwt?.claims?.sub;
    if (!userId) {
      return { statusCode: 401, headers, body: JSON.stringify({ error: 'Unauthorized' }) };
    }

    const body = JSON.parse(event.body || '{}');
    const { type, itemId, date, score } = body;

    // Whole-account snapshot: has no itemId, so it is handled before the per-item checks.
    if (type === 'state_sync') {
      return await handleStateSync(userId, body.state, headers);
    }

    if (!type || !itemId) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing required fields: type and itemId' }) };
    }

    if (!VALID_TYPES.includes(type)) {
      return { statusCode: 400, headers, body: JSON.stringify({ error: `Invalid type. Must be one of: ${VALID_TYPES.join(', ')}` }) };
    }

    const timestamp = new Date().toISOString();
    /** @type {{ type: string, meta: Record<string, unknown> } | null} */
    let adminNotification = null;

    switch (type) {
      case 'module_watched': {
        try {
          await ddb.send(new UpdateCommand({
            TableName: TABLE,
            Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#MODULE#${itemId}` },
            UpdateExpression: 'SET #status = :status, updatedAt = :updatedAt',
            // Only touch rows created by admin assign (Put); never upsert ghost tasks from video watch
            ConditionExpression: 'attribute_exists(assignedAt)',
            ExpressionAttributeNames: { '#status': 'status' },
            ExpressionAttributeValues: {
              ':status': 'in_progress',
              ':updatedAt': timestamp,
            },
          }));
        } catch (condErr) {
          if (isConditionalCheckFailed(condErr)) break;
          throw condErr;
        }
        break;
      }

      case 'module_quiz_complete': {
        const updateExpr = score !== undefined
          ? 'SET #status = :status, updatedAt = :updatedAt, completedAt = :completedAt, score = :score'
          : 'SET #status = :status, updatedAt = :updatedAt, completedAt = :completedAt';
        const exprValues = {
          ':status': 'completed',
          ':updatedAt': timestamp,
          ':completedAt': timestamp,
        };
        if (score !== undefined) {
          exprValues[':score'] = score;
        }

        try {
          const before = await ddb.send(new UpdateCommand({
            TableName: TABLE,
            Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#MODULE#${itemId}` },
            UpdateExpression: updateExpr,
            ConditionExpression: 'attribute_exists(assignedAt)',
            ExpressionAttributeNames: { '#status': 'status' },
            ExpressionAttributeValues: exprValues,
            ReturnValues: 'UPDATED_OLD',
          }));
          // Tell the supervisor about the first completion and about a retake with a new score,
          // but not about a repeated upload of the same result.
          const wasCompleted = before.Attributes?.status === 'completed';
          const sameScore = score === undefined || before.Attributes?.score === score;
          if (!(wasCompleted && sameScore)) {
            adminNotification = {
              type: 'module_quiz_complete',
              meta: score !== undefined ? { itemId, score } : { itemId },
            };
          }
        } catch (condErr) {
          if (isConditionalCheckFailed(condErr)) break;
          throw condErr;
        }
        break;
      }

      case 'lesson_complete': {
        let assignmentOk = false;
        // Update the assignment status (only if admin-assigned; no ghost lesson rows)
        try {
          await ddb.send(new UpdateCommand({
            TableName: TABLE,
            Key: { PK: `USER#${userId}`, SK: `ASSIGNMENT#LESSON#${itemId}` },
            UpdateExpression: 'SET #status = :status, updatedAt = :updatedAt, completedAt = :completedAt',
            // Already completed (a repeated call, or closed by state_sync): nothing to do and
            // no second notification for the supervisor.
            ConditionExpression: 'attribute_exists(assignedAt) AND #status <> :status',
            ExpressionAttributeNames: { '#status': 'status' },
            ExpressionAttributeValues: {
              ':status': 'completed',
              ':updatedAt': timestamp,
              ':completedAt': timestamp,
            },
          }));
          assignmentOk = true;
        } catch (condErr) {
          if (!isConditionalCheckFailed(condErr)) throw condErr;
        }

        // Upsert progress record
        await ddb.send(new UpdateCommand({
          TableName: TABLE,
          Key: { PK: `USER#${userId}`, SK: `PROGRESS#${itemId}` },
          UpdateExpression: 'SET completed = :completed, completedAt = :completedAt, updatedAt = :updatedAt',
          ExpressionAttributeValues: {
            ':completed': true,
            ':completedAt': timestamp,
            ':updatedAt': timestamp,
          },
        }));
        if (assignmentOk) {
          adminNotification = { type: 'lesson_complete', meta: { itemId } };
        }
        break;
      }

      case 'itinerary_stop_complete': {
        if (!date) {
          return { statusCode: 400, headers, body: JSON.stringify({ error: 'Missing required field: date (YYYY-MM-DD) for itinerary_stop_complete' }) };
        }

        try {
          await ddb.send(new UpdateCommand({
            TableName: TABLE,
            Key: { PK: `USER#${userId}`, SK: `ITINERARY#${date}` },
            UpdateExpression: 'ADD completedStops :stopId SET updatedAt = :updatedAt',
            // The phone uploads a visit at least once, so the same visit can arrive twice. Only
            // the first one is recorded and announced. The itinerary must exist: a visit for a
            // route the supervisor has deleted must not create an empty one.
            ConditionExpression: 'attribute_exists(PK) AND (attribute_not_exists(completedStops) OR NOT contains(completedStops, :stop))',
            ExpressionAttributeValues: {
              ':stopId': new Set([itemId]),
              ':stop': itemId,
              ':updatedAt': timestamp,
            },
          }));
          adminNotification = { type: 'itinerary_stop_complete', meta: { itemId, date } };
        } catch (condErr) {
          if (!isConditionalCheckFailed(condErr)) throw condErr;
        }
        break;
      }
    }

    if (adminNotification) {
      await emitOrgNotification(userId, adminNotification.type, adminNotification.meta);
    }

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ message: 'Progress updated', type, itemId, timestamp }),
    };
  } catch (err) {
    console.error('Error updating progress:', err);
    return { statusCode: 500, headers, body: JSON.stringify({ error: 'Failed to update progress' }) };
  }
};

// Exported for unit tests only.
exports.mergeState = mergeState;
exports.assignmentsToClose = assignmentsToClose;
exports.calculateLevel = calculateLevel;
