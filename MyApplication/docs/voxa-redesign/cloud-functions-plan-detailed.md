# Cloud Functions – Detailed Plan (XP, Notifications, Chat Metadata)

## Setup
- Use existing `firebase/functions` folder.
- Dependencies: `firebase-admin`, `firebase-functions`.
- Deploy with `firebase deploy --only functions`.

---

## 1. XP Service

### Thresholds
```js
function xpForNextLevel(level) {
  return 1000 + (level - 1) * 500;
}
```

### Callable: awardXP
```js
exports.awardXP = functions.https.onCall(async (data, context) => {
  const uid = context.auth.uid;
  const { amount, reason } = data;
  if (!uid || amount <= 0) throw new functions.https.HttpsError('invalid-argument');
  const userRef = db.collection('users').doc(uid);
  const snap = await userRef.get();
  const current = snap.data() || {};
  let newXp = (current.xp || 0) + amount;
  let newLevel = current.level || 1;
  while (newXp >= xpForNextLevel(newLevel + 1)) {
    newLevel++;
  }
  await userRef.update({ xp: newXp, level: newLevel });
  // If level up, trigger notification
  if (newLevel > (current.level || 1)) {
    await createNotification(uid, 'level_up', { level: newLevel });
    // Optional: push via FCM
    const token = current.fcmToken;
    if (token) {
      await admin.messaging().sendToDevice(token, {
        notification: { title: 'Level up!', body: `You reached level ${newLevel}` },
        data: { type: 'level_up', level: String(newLevel) }
      });
    }
  }
  return { xp: newXp, level: newLevel };
});
```

### OnUserUpdate (level recompute)
```js
exports.onUserUpdate = functions.firestore.document('users/{uid}').onUpdate(async (change, context) => {
  const before = change.before.data();
  const after = change.after.data();
  if (after.xp === before.xp) return;
  const uid = context.params.uid;
  const newLevel = computeLevel(after.xp);
  if (newLevel !== after.level) {
    await change.after.ref.update({ level: newLevel });
  }
});
```

---

## 2. Notification Triggers

### Helper: createNotification
```js
async function createNotification(uid, type, data) {
  const nid = db.collection('notifications').doc(uid).collection('items').doc().id;
  await db.collection('notifications').doc(uid).collection('items').doc(nid).set({
    type,
    data,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    read: false
  });
}
```

### onFollowCreate
```js
exports.onFollowCreate = functions.firestore.document('follows/{uid}/following/{other}').onCreate(async (snap, context) => {
  const { uid, other } = context.params;
  // Update counts (already exists)
  // Notify target if private (optional) or always
  await createNotification(other, 'follow', { followerUid: uid });
  // Award XP to follower
  await awardXPInternal(uid, 5, 'follow');
});
```

### onLikeCreate
```js
exports.onLikeCreate = functions.firestore.document('posts/{postId}/likes/{uid}').onCreate(async (snap, context) => {
  const { postId, uid } = context.params;
  const postSnap = await db.collection('posts').doc(postId).get();
  const authorUid = postSnap.data().author.uid;
  if (authorUid !== uid) {
    await createNotification(authorUid, 'post_like', { likerUid: uid, postId });
    // Award small XP
    await awardXPInternal(uid, 1, 'like');
    await awardXPInternal(authorUid, 2, 'liked');
  }
});
```

### onMessageCreate (already exists; extend for metadata)
```js
exports.onMessageCreate = functions.firestore.document('conversations/{cid}/messages/{mid}').onCreate(async (snap, context) => {
  const { cid } = context.params;
  const message = snap.data();
  const convRef = db.collection('conversations').doc(cid);
  const convSnap = await convRef.get();
  const conv = convSnap.data();
  if (!conv || !Array.isArray(conv.participants) || conv.participants.length !== 2) return;
  const senderId = message.senderId;
  const receiverId = conv.participants[0] === senderId ? conv.participants[1] : conv.participants[0];
  // Update conversation metadata
  await convRef.set({
    participants: conv.participants,
    lastMessage: message.text || (message.imageUrl ? '📷 Photo' : ''),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
  // Push to receiver (existing logic)
  const receiverDoc = await db.collection('users').doc(receiverId).get();
  const token = receiverDoc.get('fcmToken');
  const senderDoc = await db.collection('users').doc(senderId).get();
  const senderName = senderDoc.get('displayName') || 'Someone';
  if (token) {
    await admin.messaging().sendToDevice(token, {
      notification: { title: 'New message', body: `${senderName}: ${message.text || 'Photo'}` },
      data: { senderId, conversationId: cid }
    });
  }
});
```

---

## 3. Game Result Callable

### logGameResult
```js
exports.logGameResult = functions.https.onCall(async (data, context) => {
  const uid = context.auth.uid;
  const { gameId, won, score } = data;
  if (!uid || !gameId) throw new functions.https.HttpsError('invalid-argument');
  // Update user stats
  const userRef = db.collection('users').doc(uid);
  const updates = {};
  if (won) {
    updates.totalWins = admin.firestore.FieldValue.increment(1);
    updates.currentStreak = admin.firestore.FieldValue.increment(1);
  } else {
    updates.currentStreak = 0;
  }
  await userRef.update(updates);
  // XP
  const xpAmount = won ? 25 : 5;
  await awardXPInternal(uid, xpAmount, `game_${gameId}`);
  // Leaderboard (friends-only)
  const leaderboardRef = db.collection('leaderboards').doc(gameId).collection(uid).doc(uid);
  await leaderboardRef.set({
    score,
    wins: admin.firestore.FieldValue.increment(won ? 1 : 0),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
  // Optional: global leaderboard
  await db.collection('leaderboards').doc(gameId).collection('global').doc(uid).set({
    score,
    wins: admin.firestore.FieldValue.increment(won ? 1 : 0),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
  return { success: true };
});
```

---

## 4. Internal helpers

### awardXPInternal (used by triggers)
```js
async function awardXPInternal(uid, amount, reason) {
  // Reuse callable logic without auth checks
  const userRef = db.collection('users').doc(uid);
  const snap = await userRef.get();
  const current = snap.data() || {};
  let newXp = (current.xp || 0) + amount;
  let newLevel = current.level || 1;
  while (newXp >= xpForNextLevel(newLevel + 1)) {
    newLevel++;
  }
  await userRef.update({ xp: newXp, level: newLevel });
  if (newLevel > (current.level || 1)) {
    await createNotification(uid, 'level_up', { level: newLevel });
  }
}
```

---

## 5. Vibe Curated Ingest (optional admin callable)

### ingestVibeItem
```js
exports.ingestVibeItem = functions.https.onCall(async (data, context) => {
  // Only callable by admin (check custom claims)
  if (!context.auth.token.admin) throw new functions.https.HttpsError('permission-denied');
  const { title, body, media, link } = data;
  const ref = db.collection('vibe').doc();
  await ref.set({ title, body, media, link, createdAt: admin.firestore.FieldValue.serverTimestamp() });
  return { id: ref.id };
});
```

---

## Deployment Order
1. Deploy `awardXP`, `logGameResult`, `ingestVibeItem`.
2. Deploy triggers: `onFollowCreate`, `onLikeCreate`, `onMessageCreate`, `onUserUpdate`.
3. Test XP flow via callable from Android.
4. Verify notifications appear in `/notifications/{uid}/items` and via FCM.

---

## Client Integration
- Call `awardXP` from game activities on win/loss.
- Listen to `/notifications/{uid}/items` in NotificationsViewModel.
- Show confetti when a push with `type: level_up` is received.
- Use `logGameResult` to update leaderboards and XP after matches.
