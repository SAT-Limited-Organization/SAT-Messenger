const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_sat_123";
const BOB_UID = "bob_sat_456";
const CHARLIE_UID = "charlie_sat_789";

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

test("Unauthenticated user: cannot read users profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Smith",
      email: "alice@satlimited.com",
      statusMessage: "Senior Solutions Architect",
      isOnline: true,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot forge someone else's profile", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Imposter",
      email: "fake@satlimited.com",
      createdAt: new Date(),
    })
  );
});

test("Chat membership: member can read chat, non-member cannot", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_1").set({
      id: "chat_1",
      type: "direct",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("chats").doc("chat_1").get());

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("chats").doc("chat_1").get());
});

test("Chat messages: member can create message, non-member cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").collection("messages").doc("msg_1").set({
      id: "msg_1",
      chatId: "chat_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hello team SAT!",
      memberIds: [ALICE_UID, BOB_UID],
      status: "sent",
      createdAt: new Date(),
    })
  );

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("chats").doc("chat_1").collection("messages").doc("msg_2").set({
      id: "msg_2",
      chatId: "chat_1",
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Trespassing message",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );
});

test("Chat messages: member can add emoji reactions, non-member cannot", async () => {
  const msgDate = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_1").collection("messages").doc("msg_1").set({
      id: "msg_1",
      chatId: "chat_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hello team SAT!",
      memberIds: [ALICE_UID, BOB_UID],
      status: "sent",
      createdAt: msgDate,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_1").collection("messages").doc("msg_1").update({
      "reactions.bob_sat_456": "👍",
    })
  );

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("chats").doc("chat_1").collection("messages").doc("msg_1").update({
      "reactions.charlie_sat_789": "❤️",
    })
  );
});

test("Flexible Channels: follower can post only when allowFollowerPosts is true and subscribed", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("channels").doc("chan_broadcast").set({
      id: "chan_broadcast",
      name: "Exec Broadcast",
      category: "Announcements",
      isPublic: true,
      allowFollowerPosts: false,
      allowReactions: true,
      inviteCode: "CHN-EXEC01",
      creatorId: ALICE_UID,
      adminIds: [ALICE_UID],
      subscriberIds: [ALICE_UID, BOB_UID],
      followerCount: 2,
      createdAt: new Date(),
    });
    await db.collection("channels").doc("chan_open").set({
      id: "chan_open",
      name: "Open Engineering",
      category: "Engineering",
      isPublic: false,
      allowFollowerPosts: true,
      allowReactions: true,
      inviteCode: "CHN-ENG002",
      creatorId: ALICE_UID,
      adminIds: [ALICE_UID],
      subscriberIds: [ALICE_UID, BOB_UID],
      followerCount: 2,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Bob is follower, NOT admin -> denied on admin-only broadcast channel
  await assertFails(
    bobDb.collection("channels").doc("chan_broadcast").collection("posts").doc("p1").set({
      id: "p1",
      channelId: "chan_broadcast",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "Unauthorized broadcast",
      reactions: {},
      createdAt: new Date(),
    })
  );

  // Bob is follower on open channel -> allowed to post
  await assertSucceeds(
    bobDb.collection("channels").doc("chan_open").collection("posts").doc("p2").set({
      id: "p2",
      channelId: "chan_open",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "Engineering status update!",
      reactions: {},
      createdAt: new Date(),
    })
  );

  // Charlie is NOT following open channel -> denied to post
  await assertFails(
    charlieDb.collection("channels").doc("chan_open").collection("posts").doc("p3").set({
      id: "p3",
      channelId: "chan_open",
      authorId: CHARLIE_UID,
      authorName: "Charlie",
      text: "Not subscribed",
      reactions: {},
      createdAt: new Date(),
    })
  );

  // Bob cannot update channel settings
  await assertFails(
    bobDb.collection("channels").doc("chan_open").update({
      allowFollowerPosts: false,
    })
  );
});

test("Invite Links: non-member can join chatRoom only via active invite link", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("invites").doc("GRP-ACTIVE").set({
      code: "GRP-ACTIVE",
      targetType: "group",
      targetId: "room_invited",
      targetName: "SAT Mission Room",
      creatorId: ALICE_UID,
      active: true,
      usesCount: 0,
      createdAt: new Date(),
    });
    await db.collection("chatRooms").doc("room_invited").set({
      id: "room_invited",
      name: "SAT Mission Room",
      inviteCode: "GRP-ACTIVE",
      memberIds: [ALICE_UID],
      adminIds: [ALICE_UID],
      createdBy: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    await db.collection("chatRooms").doc("room_no_invite").set({
      id: "room_no_invite",
      name: "Private Vault",
      memberIds: [ALICE_UID],
      adminIds: [ALICE_UID],
      createdBy: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Charlie can look up invite document by code
  await assertSucceeds(charlieDb.collection("invites").doc("GRP-ACTIVE").get());
  // Charlie cannot list invites created by Alice
  await assertFails(charlieDb.collection("invites").get());

  // Charlie cannot join a room without an active invite
  await assertFails(
    charlieDb.collection("chatRooms").doc("room_no_invite").update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );

  // Charlie CAN add ONLY himself to room_invited because GRP-ACTIVE is active
  await assertSucceeds(
    charlieDb.collection("chatRooms").doc("room_invited").update({
      memberIds: [ALICE_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );
});

test("Voice messages: chat member can send audio clip with durationSeconds, non-member or invalid duration denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_voice").collection("messages").doc("voice_1").set({
      id: "voice_1",
      chatId: "chat_voice",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Voice message",
      type: "audio",
      attachmentUrl: "data:audio/mp4;base64,AAAAIGZ0eXBpc29t",
      fileName: "Voice_1.m4a",
      fileSize: 2048,
      durationSeconds: 12,
      memberIds: [ALICE_UID, BOB_UID],
      status: "sent",
      deleted: false,
      createdAt: new Date(),
    })
  );

  // Invalid duration (> 600s) is rejected
  await assertFails(
    aliceDb.collection("chats").doc("chat_voice").collection("messages").doc("voice_bad").set({
      id: "voice_bad",
      chatId: "chat_voice",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Voice message",
      type: "audio",
      attachmentUrl: "data:audio/mp4;base64,AAAAIGZ0eXBpc29t",
      durationSeconds: 999,
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );

  // Non-member cannot send voice message
  await assertFails(
    charlieDb.collection("chats").doc("chat_voice").collection("messages").doc("voice_2").set({
      id: "voice_2",
      chatId: "chat_voice",
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Voice message",
      type: "audio",
      attachmentUrl: "data:audio/mp4;base64,AAAAIGZ0eXBpc29t",
      durationSeconds: 5,
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );
});

test("Contacts & Availability: user can sync valid availabilityStatus and manage friends, cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const pastDate = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Smith",
      email: "alice@satlimited.com",
      availabilityStatus: "In a Meeting",
      isOnline: true,
      lastSeen: pastDate,
      createdAt: pastDate,
      updatedAt: pastDate,
    })
  );

  // Invalid availabilityStatus is rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      availabilityStatus: "On Mars",
    })
  );

  // Alice can bookmark Bob as a friend in her contacts subcollection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc(BOB_UID).set({
      contactUserId: BOB_UID,
      relationship: "friend",
      createdAt: pastDate,
      updatedAt: pastDate,
    })
  );

  // Bob cannot read or modify Alice's saved contacts subcollection
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("contacts").doc(BOB_UID).get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("contacts").doc(CHARLIE_UID).set({
      contactUserId: CHARLIE_UID,
      relationship: "friend",
      createdAt: pastDate,
    })
  );
});

test("Group Room Admin Promotion: creator/admin can promote and dismiss admins, non-admin and creator demotion denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chatRooms").doc("room_admins").set({
      id: "room_admins",
      name: "Mission Control",
      memberIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      adminIds: [ALICE_UID],
      createdBy: ALICE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Non-admin Bob cannot promote himself
  await assertFails(
    bobDb.collection("chatRooms").doc("room_admins").update({
      adminIds: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Creator Alice promotes Bob to admin
  await assertSucceeds(
    aliceDb.collection("chatRooms").doc("room_admins").update({
      adminIds: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Promoted Admin Bob cannot remove Creator Alice from adminIds
  await assertFails(
    bobDb.collection("chatRooms").doc("room_admins").update({
      adminIds: [BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Promoted Admin Bob can promote Charlie
  await assertSucceeds(
    bobDb.collection("chatRooms").doc("room_admins").update({
      adminIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );

  // Creator Alice can dismiss Bob back to standard member
  await assertSucceeds(
    aliceDb.collection("chatRooms").doc("room_admins").update({
      adminIds: [ALICE_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );

  // Demoted Bob can no longer alter adminIds
  await assertFails(
    bobDb.collection("chatRooms").doc("room_admins").update({
      adminIds: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );
  // Existing admin Charlie can still manage admins
  await assertSucceeds(
    charlieDb.collection("chatRooms").doc("room_admins").update({
      adminIds: [ALICE_UID],
      updatedAt: new Date(),
    })
  );
});

test("Channel Admin Promotion: creator/admin can update adminIds, creator stays protected, non-admin denied", async () => {
  const createdAt = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("channels").doc("chan_admins").set({
      id: "chan_admins",
      name: "Engineering Dispatch",
      category: "Engineering",
      isPublic: true,
      allowFollowerPosts: false,
      allowReactions: true,
      creatorId: ALICE_UID,
      adminIds: [ALICE_UID],
      subscriberIds: [ALICE_UID, BOB_UID],
      followerCount: 2,
      createdAt,
      updatedAt: createdAt,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Follower Bob cannot promote himself
  await assertFails(
    bobDb.collection("channels").doc("chan_admins").update({
      adminIds: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Creator Alice promotes Bob to Channel Admin
  await assertSucceeds(
    aliceDb.collection("channels").doc("chan_admins").update({
      adminIds: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Admin Bob cannot demote Creator Alice
  await assertFails(
    bobDb.collection("channels").doc("chan_admins").update({
      adminIds: [BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Creator Alice can dismiss Bob
  await assertSucceeds(
    aliceDb.collection("channels").doc("chan_admins").update({
      adminIds: [ALICE_UID],
      updatedAt: new Date(),
    })
  );
});

test("Community Admin Promotion: creator/admin can promote/dismiss admins, non-admin and creator removal denied", async () => {
  const createdAt = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("communities").doc("comm_admins").set({
      id: "comm_admins",
      name: "SAT Orbital Ops",
      creatorId: ALICE_UID,
      adminIds: [ALICE_UID],
      memberIds: [ALICE_UID, BOB_UID],
      subgroupChatIds: [],
      createdAt,
      updatedAt: createdAt,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Member Bob cannot self-promote
  await assertFails(
    bobDb.collection("communities").doc("comm_admins").update({
      adminIds: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Creator Alice promotes Bob
  await assertSucceeds(
    aliceDb.collection("communities").doc("comm_admins").update({
      adminIds: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Promoted Admin Bob cannot remove Creator Alice
  await assertFails(
    bobDb.collection("communities").doc("comm_admins").update({
      adminIds: [BOB_UID],
      updatedAt: new Date(),
    })
  );

  // Creator Alice dismisses Bob
  await assertSucceeds(
    aliceDb.collection("communities").doc("comm_admins").update({
      adminIds: [ALICE_UID],
      updatedAt: new Date(),
    })
  );
});

test("Video media messages: chat member can send type video attachment, non-member denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_video").collection("messages").doc("vid_1").set({
      id: "vid_1",
      chatId: "chat_video",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Flight telemetry video",
      type: "video",
      attachmentUrl: "data:video/mp4;base64,AAAAIGZ0eXBpc29t",
      fileName: "telemetry_clip.mp4",
      fileSize: 4096,
      memberIds: [ALICE_UID, BOB_UID],
      status: "sent",
      deleted: false,
      createdAt: new Date(),
    })
  );

  await assertFails(
    charlieDb.collection("chats").doc("chat_video").collection("messages").doc("vid_2").set({
      id: "vid_2",
      chatId: "chat_video",
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Unauthorized video",
      type: "video",
      attachmentUrl: "data:video/mp4;base64,AAAAIGZ0eXBpc29t",
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );
});


