/**
 * Tests for the account state merge in user/updateProgress ("state_sync").
 * Run from lambda-functions/:  node --test _tests/stateSync.test.js
 * (Lives outside the function folders, so deploy.sh never packages it.)
 */
const test = require('node:test');
const assert = require('node:assert/strict');

process.env.AWS_REGION = process.env.AWS_REGION || 'us-east-1';
const { mergeState, calculateLevel, assignmentsToClose } = require('../user/updateProgress/index.js');

test('XP never goes down: the largest of stored state, profile and upload wins', () => {
  assert.equal(mergeState({ totalPoints: 100 }, { totalPoints: 160 }, { totalPoints: 150 }).totalPoints, 160);
  assert.equal(mergeState({ totalPoints: 300 }, { totalPoints: 160 }, { totalPoints: 40 }).totalPoints, 300);
  assert.equal(mergeState({}, {}, { totalPoints: 75 }).totalPoints, 75);
});

test('level and rank follow the merged XP using the app thresholds', () => {
  assert.equal(calculateLevel(0), 0);
  assert.equal(calculateLevel(99), 0);
  assert.equal(calculateLevel(100), 1);
  assert.equal(calculateLevel(500), 3);
  assert.equal(calculateLevel(4600), 9);
  assert.equal(calculateLevel(4600 + 1200), 10);
  const merged = mergeState({}, {}, { totalPoints: 520 });
  assert.equal(merged.level, 3);
  assert.equal(merged.rank, 'Novice');
});

test('id sets are unioned, de-duplicated and sorted', () => {
  const merged = mergeState(
    { watchedVideos: ['b', 'a'], completedLessons: ['lesson-1'] },
    {},
    { watchedVideos: ['c', 'a'], completedQuizzes: ['q1'], completedLessons: [], reportDates: ['2026-11-12'] }
  );
  assert.deepEqual(merged.watchedVideos, ['a', 'b', 'c']);
  assert.deepEqual(merged.completedQuizzes, ['q1']);
  assert.deepEqual(merged.completedLessons, ['lesson-1']);
  assert.deepEqual(merged.reportDates, ['2026-11-12']);
});

test('the streak entry with the later activity day wins; the best streak is kept', () => {
  const newer = mergeState(
    { currentStreak: 2, lastStreakDate: '2026-11-10', longestStreak: 6 },
    {},
    { currentStreak: 3, lastStreakDate: '2026-11-11' }
  );
  assert.equal(newer.currentStreak, 3);
  assert.equal(newer.lastStreakDate, '2026-11-11');
  assert.equal(newer.longestStreak, 6);

  // An old device uploading stale data cannot rewind the streak.
  const stale = mergeState(
    { currentStreak: 3, lastStreakDate: '2026-11-11' },
    {},
    { currentStreak: 9, lastStreakDate: '2026-11-01' }
  );
  assert.equal(stale.currentStreak, 3);
  assert.equal(stale.lastStreakDate, '2026-11-11');

  const sameDay = mergeState({ currentStreak: 3, lastStreakDate: '2026-11-11' }, {}, { currentStreak: 4, lastStreakDate: '2026-11-11' });
  assert.equal(sameDay.currentStreak, 4);
});

test('daily questions: the later day wins, a bad entry is ignored', () => {
  const kept = mergeState(
    { dailyQuestions: { date: '2026-11-12', correct: 3, total: 3, xp: 85 } },
    {},
    { dailyQuestions: { date: '2026-11-10', correct: 1, total: 3, xp: 45 } }
  );
  assert.deepEqual(kept.dailyQuestions, { date: '2026-11-12', correct: 3, total: 3, xp: 85 });

  const replaced = mergeState({}, {}, { dailyQuestions: { date: '2026-11-12', correct: 2, total: 3, xp: 65 } });
  assert.deepEqual(replaced.dailyQuestions, { date: '2026-11-12', correct: 2, total: 3, xp: 65 });

  assert.equal(mergeState({}, {}, { dailyQuestions: { date: 'yesterday' } }).dailyQuestions, null);
});

test('lives come from the device that is reporting and stay within 0..10', () => {
  assert.equal(mergeState({ lives: 7 }, { lives: 10 }, { lives: 4 }).lives, 4);
  assert.equal(mergeState({ lives: 7 }, { lives: 10 }, {}).lives, 7);
  assert.equal(mergeState({}, { lives: 9 }, {}).lives, 9);
  assert.equal(mergeState({}, {}, {}).lives, 10);
  assert.equal(mergeState({}, {}, { lives: 99 }).lives, 10);
  assert.equal(mergeState({}, {}, { lives: 0 }).lives, 0);
});

test('garbage in the upload cannot corrupt the stored state', () => {
  const merged = mergeState(
    { totalPoints: 200, watchedVideos: ['a'] },
    {},
    { totalPoints: 'lots', lives: -3, watchedVideos: 'a,b', completedLessons: [1, null, 'lesson-2', ''], currentStreak: -5, lastStreakDate: 12 }
  );
  assert.equal(merged.totalPoints, 200);
  assert.equal(merged.lives, 0);
  assert.deepEqual(merged.watchedVideos, ['a']);
  assert.deepEqual(merged.completedLessons, ['lesson-2']);
  assert.equal(merged.currentStreak, 0);
  assert.equal(merged.lastStreakDate, '');
});

// ---- assignments finished offline ----

const assigned = (extra) => ({ PK: 'USER#u1', assignedAt: '2026-11-12T08:00:00.000Z', status: 'assigned', ...extra });

test('a lesson or quiz finished offline closes the supervisor\'s assignment', () => {
  const rows = [
    assigned({ SK: 'ASSIGNMENT#LESSON#lesson-8', type: 'lesson', lessonId: 'lesson-8' }),
    assigned({ SK: 'ASSIGNMENT#LESSON#lesson-3', type: 'lesson', lessonId: 'lesson-3' }),
    assigned({ SK: 'ASSIGNMENT#MODULE#mod2-malnutrition', type: 'module', moduleId: 'mod2-malnutrition' }),
    assigned({ SK: 'ASSIGNMENT#MODULE#mod4-pregnancy', type: 'module', moduleId: 'mod4-pregnancy', status: 'in_progress' }),
  ];
  const state = { completedLessons: ['lesson-8'], completedQuizzes: ['mod4-pregnancy'], watchedVideos: ['mod2-malnutrition'] };

  const closed = assignmentsToClose(rows, state).map((a) => a.SK);

  // lesson-3 is not done; watching the malnutrition video is not finishing its quiz.
  assert.deepEqual(closed, ['ASSIGNMENT#LESSON#lesson-8', 'ASSIGNMENT#MODULE#mod4-pregnancy']);
});

test('assignments that are already completed are not touched again', () => {
  const rows = [assigned({ SK: 'ASSIGNMENT#LESSON#lesson-8', type: 'lesson', lessonId: 'lesson-8', status: 'completed' })];
  assert.deepEqual(assignmentsToClose(rows, { completedLessons: ['lesson-8'] }), []);
});

test('rows that no admin created, and other task types, are ignored', () => {
  const rows = [
    // Ghost row from an old upsert: no assignedAt.
    { PK: 'USER#u1', SK: 'ASSIGNMENT#LESSON#lesson-8', type: 'lesson', lessonId: 'lesson-8', status: 'assigned' },
    assigned({ SK: 'ASSIGNMENT#REPORT#daily', type: 'report' }),
    assigned({ SK: 'ASSIGNMENT#LESSON#', type: 'lesson' }),
    null,
  ];
  assert.deepEqual(assignmentsToClose(rows, { completedLessons: ['lesson-8'], completedQuizzes: ['daily'] }), []);
  assert.deepEqual(assignmentsToClose(undefined, { completedLessons: ['lesson-8'] }), []);
  assert.deepEqual(assignmentsToClose([assigned({ SK: 'x', type: 'lesson', lessonId: 'lesson-8' })], {}), []);
  assert.deepEqual(assignmentsToClose([assigned({ SK: 'x', type: 'lesson', lessonId: 'lesson-8' })], { completedLessons: 'lesson-8' }), []);
});

test('a video assignment follows the same rule as a module assignment', () => {
  const rows = [assigned({ SK: 'ASSIGNMENT#MODULE#mod1-nervous-system', type: 'video', moduleId: 'mod1-nervous-system' })];
  assert.equal(assignmentsToClose(rows, { completedQuizzes: ['mod1-nervous-system'] }).length, 1);
});
