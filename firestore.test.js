const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any CalmFlow collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Hack",
      description: "",
      priority: "HIGH",
      cognitiveLoad: 3,
      category: "DEEP_WORK",
      completed: false,
      dueAtMillis: 0,
      reminderAtMillis: 0,
      estimatedMinutes: 25,
      xpReward: 50,
      completedAtMillis: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("UserProfile: owner create & partial update succeed; ghost fields & cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      xp: 0,
      level: 1,
      calmCoins: 50,
      streakDays: 1,
      totalFocusMinutes: 0,
      totalTasksCompleted: 0,
      cortisolScore: 38,
      unlockedBadges: ["FIRST_STEP"],
      lastActiveDate: "2026-11-17",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update on mutable stats
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 120,
      level: 2,
      calmCoins: 75,
      cortisolScore: 28,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 200,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject cross-user access
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ xp: 999, updatedAt: new Date(Date.now() - 500) }));
});

test("Tasks: owner CRUD, partial toggle, list, and rejection of @DocumentId 'id' or invalid priority", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Empty list query succeeds for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get());

  // Create valid task (without 'id' field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Design neural architecture",
      description: "Focus block before noon",
      priority: "CRITICAL",
      cognitiveLoad: 4,
      category: "DEEP_WORK",
      completed: false,
      dueAtMillis: Date.now() + 86400000,
      reminderAtMillis: Date.now() + 3600000,
      estimatedMinutes: 50,
      xpReward: 120,
      completedAtMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update: mark completed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").update({
      completed: true,
      completedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject unexpected 'id' field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_2").set({
      id: "task_2",
      userId: ALICE_UID,
      title: "Bad id payload",
      description: "",
      priority: "LOW",
      cognitiveLoad: 1,
      category: "ADMIN",
      completed: false,
      dueAtMillis: 0,
      reminderAtMillis: 0,
      estimatedMinutes: 15,
      xpReward: 20,
      completedAtMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Cross-user list/read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").delete());
});

test("Notes, CortisolLogs, FocusSessions, and Rewards enforce bounds and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Note
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Evening brain dump",
      content: "Cleared all open loops before sleep.",
      tag: "BRAIN_DUMP",
      pinned: true,
      linkedTaskTitle: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").update({
      pinned: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // CortisolLog (immutable once created)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cortisolLogs").doc("log_1").set({
      userId: ALICE_UID,
      level: 34,
      trigger: "Box Breathing",
      mood: "SERENE",
      note: "4-4-4-4 reset worked well",
      loggedAtMillis: Date.now() - 1000,
      createdAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cortisolLogs").doc("log_1").update({ level: 10 })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cortisolLogs").doc("log_2").set({
      userId: ALICE_UID,
      level: 150,
      trigger: "Invalid",
      mood: "SERENE",
      note: "",
      loggedAtMillis: Date.now() - 1000,
      createdAt: now,
    })
  );

  // FocusSession
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("focusSessions").doc("sess_1").set({
      userId: ALICE_UID,
      taskTitle: "Deep Research",
      durationMinutes: 25,
      completed: true,
      cortisolDelta: -8,
      xpEarned: 75,
      startedAtMillis: Date.now() - 1500000,
      createdAt: now,
    })
  );

  // Reward
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("rewards").doc("rew_1").set({
      userId: ALICE_UID,
      title: "Espresso Break",
      costCoins: 40,
      iconKey: "coffee",
      redeemedCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("rewards").doc("rew_1").update({
      redeemedCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("rewards").get());
});
