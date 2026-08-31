const { https, firestore, auth, database, pubsub } = require("firebase-functions/v2");

// HTTP Function Example
/*
exports.helloWorld = https.onRequest((req, res) => {
  res.status(200).send("Hello, World!");
});
*/

// Firestore Trigger Example
/*
exports.firestoreExample = firestore.document("example/{docId}").onWrite((event) => {
  console.log("Firestore Triggered:", event.params.docId);
  return null;
});
*/

// Realtime Database Trigger Example
/*
exports.realtimeDbExample = database.ref("/example/{itemId}").onWrite((change, context) => {
  console.log("Realtime Database Triggered for path:", context.params.itemId);
  return null;
});
*/

// Authentication Trigger Example
/*
exports.authExample = auth.user().onCreate((user) => {
  console.log("New user created:", user.uid);
  return null;
});
*/

// Firestore Data Comparison Trigger
/*
exports.firestoreDataTrigger = firestore
  .document("orders/{orderId}")
  .onUpdate((change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();
    console.log("Firestore Change detected:", { beforeData, afterData });
    return null;
  });
*/

// Realtime Database Detailed Change Logging
/*
exports.customDbTrigger = database
  .ref("/user_updates/{updateId}")
  .onUpdate((change, context) => {
    console.log("Realtime DB update detected:", context.params.updateId);
    console.log("Before:", change.before.val());
    console.log("After:", change.after.val());
    return null;
  });
*/

// Pub/Sub Example
/*
exports.pubSubExample = pubsub.topic("my-topic").onPublish((message) => {
  console.log("Pub/Sub trigger received a message:", message.json);
  return null;
});
*/