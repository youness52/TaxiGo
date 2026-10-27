const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

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

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Unauthenticated user: cannot read rides subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("rides").get());
});

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Traveler",
      email: "alice@example.com",
      walletBalance: 25.5,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create profile with mismatched userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: BOB_UID, // Mismatched UID
      displayName: "Alice Impersonator",
      email: "alice@example.com",
      walletBalance: 10.0,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot read Bob's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Driver",
      email: "bob@example.com",
      walletBalance: 50.0,
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: can create and read their own ride", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const rideRef = aliceDb.collection("users").doc(ALICE_UID).collection("rides").doc("ride_1");

  await assertSucceeds(
    rideRef.set({
      userId: ALICE_UID,
      pickupAddress: "100 Market St",
      destinationAddress: "SFO Airport Terminal 2",
      rideTier: "comfort",
      fare: 34.50,
      paymentMethod: "card",
      status: "SEARCHING",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(rideRef.get());
});

test("Authenticated user: cannot write ride into Bob's subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobRideRef = aliceDb.collection("users").doc(BOB_UID).collection("rides").doc("ride_bob");

  await assertFails(
    bobRideRef.set({
      userId: BOB_UID,
      pickupAddress: "100 Market St",
      destinationAddress: "SFO Airport Terminal 2",
      rideTier: "economy",
      fare: 22.00,
      paymentMethod: "wallet",
      status: "SEARCHING",
      createdAt: new Date(),
    })
  );
});

test("Invalid ride payload (negative fare): fails validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const rideRef = aliceDb.collection("users").doc(ALICE_UID).collection("rides").doc("ride_invalid");

  await assertFails(
    rideRef.set({
      userId: ALICE_UID,
      pickupAddress: "100 Market St",
      destinationAddress: "SFO Airport Terminal 2",
      rideTier: "economy",
      fare: -15.00, // Negative fare!
      paymentMethod: "card",
      status: "SEARCHING",
      createdAt: new Date(),
    })
  );
});
