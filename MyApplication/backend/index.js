const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');
const bodyParser = require('body-parser');
const multer = require('multer');
require('dotenv').config();

// Initialize Firebase Admin
if (!admin.apps.length) {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '{}');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    storageBucket: process.env.FIREBASE_STORAGE_BUCKET
  });
}
const db = admin.firestore();
const fcm = admin.messaging();
const bucket = admin.storage().bucket();

const app = express();
app.use(cors());
app.use(bodyParser.json());
const upload = multer({ storage: multer.memoryStorage() });

// Helper: award XP and recompute level
async function awardXP(uid, amount, reason) {
  const userRef = db.collection('users').doc(uid);
  const snap = await userRef.get();
  const data = snap.data() || {};
  let newXp = (data.xp || 0) + amount;
  let newLevel = data.level || 1;
  while (newXp >= 1000 + newLevel * 500) {
    newLevel++;
  }
  await userRef.update({ xp: newXp, level: newLevel });
  // Level-up notification
  if (newLevel > (data.level || 1)) {
    await db.collection('notifications').doc(uid).collection('items').doc().set({
      type: 'level_up',
      data: { level: newLevel },
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      read: false
    });
    const token = data.fcmToken;
    if (token) {
      await fcm.sendToDevice(token, {
        notification: { title: 'Level up!', body: `You reached level ${newLevel}` },
        data: { type: 'level_up', level: String(newLevel) }
      });
    }
  }
  return { xp: newXp, level: newLevel };
}

// Helper: create notification
async function createNotification(uid, type, dataObj) {
  await db.collection('notifications').doc(uid).collection('items').doc().set({
    type,
    data: dataObj,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    read: false
  });
}

// Auth middleware
async function authMiddleware(req, res, next) {
  const token = req.headers.authorization?.split('Bearer ')[1];
  if (!token) return res.status(401).json({ error: 'Unauthorized' });
  try {
    const decoded = await admin.auth().verifyIdToken(token);
    req.user = decoded;
    next();
  } catch (e) {
    res.status(401).json({ error: 'Invalid token' });
  }
}

// Follow creation (mirror onFollowCreate)
app.post('/follow', authMiddleware, async (req, res) => {
  const { other } = req.body;
  const uid = req.user.uid;
  if (!other) return res.status(400).json({ error: 'Missing other' });
  try {
    const batch = db.batch();
    batch.update(db.collection('users').doc(uid), { followingCount: admin.firestore.FieldValue.increment(1) });
    batch.update(db.collection('users').doc(other), { followersCount: admin.firestore.FieldValue.increment(1) });
    await batch.commit();
    // Create follow relationship
    await db.collection('follows').doc(uid).collection('following').doc(other).set({
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    await db.collection('follows').doc(other).collection('followers').doc(uid).set({
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    // Notify target
    await createNotification(other, 'follow', { followerUid: uid });
    // Award small XP
    await awardXP(uid, 5, 'follow');
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Unfollow (mirror onFollowDelete)
app.post('/unfollow', authMiddleware, async (req, res) => {
  const { other } = req.body;
  const uid = req.user.uid;
  if (!other) return res.status(400).json({ error: 'Missing other' });
  try {
    const batch = db.batch();
    batch.update(db.collection('users').doc(uid), { followingCount: admin.firestore.FieldValue.increment(-1) });
    batch.update(db.collection('users').doc(other), { followersCount: admin.firestore.FieldValue.increment(-1) });
    await batch.commit();
    // Delete follow relationships
    await db.collection('follows').doc(uid).collection('following').doc(other).delete();
    await db.collection('follows').doc(other).collection('followers').doc(uid).delete();
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Message metadata update (mirror onMessageCreate)
app.post('/message', authMiddleware, async (req, res) => {
  const { cid, senderId, text, imageUrl } = req.body;
  if (!cid || !senderId) return res.status(400).json({ error: 'Missing cid or senderId' });
  try {
    const convRef = db.collection('conversations').doc(cid);
    const convSnap = await convRef.get();
    if (!convSnap.exists) return res.status(404).json({ error: 'Conversation not found' });
    const conv = convSnap.data();
    const participants = conv.participants || [];
    const receiverId = participants.find(p => p !== senderId);
    const lastMessage = text || (imageUrl ? '📷 Photo' : '');
    await convRef.set({
      participants,
      lastMessage,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    // Push notification to receiver
    if (receiverId) {
      const receiverDoc = await db.collection('users').doc(receiverId).get();
      const token = receiverDoc.get('fcmToken');
      const senderDoc = await db.collection('users').doc(senderId).get();
      const senderName = senderDoc.get('displayName') || 'Someone';
      if (token) {
        await fcm.sendToDevice(token, {
          notification: { title: 'New message', body: `${senderName}: ${lastMessage}` },
          data: { senderId, conversationId: cid }
        });
      }
    }
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Award XP
app.post('/awardXP', authMiddleware, async (req, res) => {
  const { amount, reason } = req.body;
  if (amount == null) return res.status(400).json({ error: 'Missing amount' });
  try {
    const result = await awardXP(req.user.uid, amount, reason);
    res.json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Game result (XP + leaderboard)
app.post('/gameResult', authMiddleware, async (req, res) => {
  const { gameId, won, score } = req.body;
  if (!gameId) return res.status(400).json({ error: 'Missing gameId' });
  try {
    const uid = req.user.uid;
    const userRef = db.collection('users').doc(uid);
    const updates = {};
    if (won) {
      updates.totalWins = admin.firestore.FieldValue.increment(1);
      updates.currentStreak = admin.firestore.FieldValue.increment(1);
    } else {
      updates.currentStreak = 0;
    }
    await userRef.update(updates);
    const xpAmount = won ? 25 : 5;
    const xpResult = await awardXP(uid, xpAmount, `game_${gameId}`);
    // Leaderboard
    await db.collection('leaderboards').doc(gameId).collection(uid).doc(uid).set({
      score,
      wins: admin.firestore.FieldValue.increment(won ? 1 : 0),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    res.json({ success: true, xp: xpResult });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Create post
app.post('/post', authMiddleware, upload.single('media'), async (req, res) => {
  const { text } = req.body;
  const uid = req.user.uid;
  try {
    let mediaUrl = null;
    if (req.file) {
      const fileName = `posts/${uid}/${Date.now()}_${req.file.originalname}`;
      const file = bucket.file(fileName);
      await file.save(req.file.buffer, { metadata: { contentType: req.file.mimetype } });
      await file.makePublic();
      mediaUrl = `https://storage.googleapis.com/${bucket.name}/${fileName}`;
    }
    const postRef = await db.collection('posts').add({
      author: { uid, displayName: req.user.name || '', photoUrl: req.user.picture || '' },
      text: text || '',
      mediaUrl,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      likesCount: 0,
      commentsCount: 0
    });
    res.json({ postId: postRef.id });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Like post
app.post('/like', authMiddleware, async (req, res) => {
  const { postId } = req.body;
  const uid = req.user.uid;
  if (!postId) return res.status(400).json({ error: 'Missing postId' });
  try {
    const likeRef = db.collection('posts').doc(postId).collection('likes').doc(uid);
    const exists = (await likeRef.get()).exists;
    if (exists) {
      await likeRef.delete();
      await db.collection('posts').doc(postId).update({ likesCount: admin.firestore.FieldValue.increment(-1) });
      res.json({ liked: false });
    } else {
      await likeRef.set({ createdAt: admin.firestore.FieldValue.serverTimestamp() });
      await db.collection('posts').doc(postId).update({ likesCount: admin.firestore.FieldValue.increment(1) });
      // Notify author
      const postSnap = await db.collection('posts').doc(postId).get();
      const authorUid = postSnap.data().author.uid;
      if (authorUid !== uid) {
        await createNotification(authorUid, 'post_like', { likerUid: uid, postId });
        await awardXP(uid, 1, 'like');
        await awardXP(authorUid, 2, 'liked');
      }
      res.json({ liked: true });
    }
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Add comment
app.post('/comment', authMiddleware, async (req, res) => {
  const { postId, text } = req.body;
  const uid = req.user.uid;
  if (!postId || !text) return res.status(400).json({ error: 'Missing postId or text' });
  try {
    const commentRef = await db.collection('posts').doc(postId).collection('comments').add({
      author: { uid, displayName: req.user.name || '', photoUrl: req.user.picture || '' },
      text,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    await db.collection('posts').doc(postId).update({ commentsCount: admin.firestore.FieldValue.increment(1) });
    // Notify author
    const postSnap = await db.collection('posts').doc(postId).get();
    const authorUid = postSnap.data().author.uid;
    if (authorUid !== uid) {
      await createNotification(authorUid, 'post_comment', { commenterUid: uid, postId, commentId: commentRef.id });
    }
    res.json({ commentId: commentRef.id });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Fetch feed (posts + pagination)
app.get('/feed', authMiddleware, async (req, res) => {
  const { limit = 20, startAfter } = req.query;
  try {
    let q = db.collection('posts').orderBy('createdAt', 'desc').limit(parseInt(limit));
    if (startAfter) q = q.startAfter(startAfter);
    const snap = await q.get();
    const posts = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    res.json({ posts });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Fetch notifications
app.get('/notifications', authMiddleware, async (req, res) => {
  const { limit = 50 } = req.query;
  try {
    const snap = await db.collection('notifications').doc(req.user.uid).collection('items')
      .orderBy('createdAt', 'desc').limit(parseInt(limit)).get();
    const notifications = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    res.json({ notifications });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Update profile
app.post('/profile', authMiddleware, upload.fields([{ name: 'avatar' }, { name: 'banner' }]), async (req, res) => {
  const { displayName, bio, isPrivate } = req.body;
  const uid = req.user.uid;
  try {
    const update = {};
    if (displayName != null) update.displayName = displayName;
    if (bio != null) update.bio = bio;
    if (isPrivate != null) update.isPrivate = isPrivate === 'true';
    if (req.files.avatar) {
      const fileName = `profile_images/${uid}/${Date.now()}_${req.files.avatar[0].originalname}`;
      const file = bucket.file(fileName);
      await file.save(req.files.avatar[0].buffer, { metadata: { contentType: req.files.avatar[0].mimetype } });
      await file.makePublic();
      update.photoUrl = `https://storage.googleapis.com/${bucket.name}/${fileName}`;
    }
    if (req.files.banner) {
      const fileName = `banners/${uid}/${Date.now()}_${req.files.banner[0].originalname}`;
      const file = bucket.file(fileName);
      await file.save(req.files.banner[0].buffer, { metadata: { contentType: req.files.banner[1].mimetype } });
      await file.makePublic();
      update.bannerUrl = `https://storage.googleapis.com/${bucket.name}/${fileName}`;
    }
    await db.collection('users').doc(uid).update(update);
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Health check
app.get('/health', (req, res) => res.json({ ok: true, ts: new Date().toISOString() }));

// Export for Vercel
module.exports = app;
