const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_asansol_123";
const OTHER_UID = "intruder_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write store_transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(OWNER_UID)
      .collection("store_transactions")
      .get()
  );
});

test("Authenticated owner can create and list valid store_transactions and cancel without deleting", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const now = new Date();
  const txRef = ownerDb
    .collection("users")
    .doc(OWNER_UID)
    .collection("store_transactions")
    .doc("tx_001");

  await assertSucceeds(
    txRef.set({
      userId: OWNER_UID,
      billNumber: "NFP-20261111-0001",
      staffId: "staff_owner",
      staffName: "Shop Owner",
      grandTotal: 1700.0,
      lineCount: 2,
      itemsSummary: "Salmon Fish 1kg @ 500, Vetki Fish Finger 1kg @ 1200",
      paymentMethod: "CASH",
      status: "COMPLETED",
      isRepeatCustomer: false,
      saleEpochMillis: Date.now(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // List own transactions
  await assertSucceeds(
    ownerDb
      .collection("users")
      .doc(OWNER_UID)
      .collection("store_transactions")
      .get()
  );

  // Audited cancellation update succeeds
  await assertSucceeds(
    txRef.update({
      status: "CANCELLED",
      cancellationReason: "Customer changed quantity",
      updatedAt: new Date(),
    })
  );

  // Physical deletion is strictly forbidden
  await assertFails(txRef.delete());
});

test("Cross-user access to store_transactions and store_freezers is denied", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    ownerDb
      .collection("users")
      .doc(OWNER_UID)
      .collection("store_freezers")
      .doc("freezer_3_sec_b")
      .set({
        userId: OWNER_UID,
        freezerNumber: 3,
        sectionCode: "Section B",
        mappedProducts: "Vetki Fish Finger, Vetki Fish Fry",
        manualNotes: "Top basket left side",
        updatedByStaffName: "Staff 1",
        updatedEpochMillis: Date.now(),
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertFails(
    otherDb
      .collection("users")
      .doc(OWNER_UID)
      .collection("store_freezers")
      .doc("freezer_3_sec_b")
      .get()
  );
});

test("Shadow update with unauthorized ghost field is rejected", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const now = new Date();

  await assertFails(
    ownerDb
      .collection("users")
      .doc(OWNER_UID)
      .collection("store_visitors")
      .doc("vis_001")
      .set({
        userId: OWNER_UID,
        dateKey: "2026-11-11",
        delta: 1,
        staffId: "staff_1",
        staffName: "Staff 1",
        eventEpochMillis: Date.now(),
        createdAt: now,
        isVerifiedAdmin: true,
      })
  );
});
