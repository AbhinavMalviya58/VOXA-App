// Vercel serverless replacement for Firebase Cloud Functions (no billing)
// Assumes you set FIREBASE_PROJECT_ID and FIREBASE_SERVICE_ACCOUNT_JSON env vars on Vercel
const admin = require('firebase-admin');
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: `https://${process.env.FIREBASE_PROJECT_ID}-default-rtdb.firebaseio.com`
});

const db = admin.firestore();
const fcm = admin.messaging();

// Helper: award XP and recompute level
async function awardXP(uid, amount, reason) {
  const userRef = db.collection('users').doc(uid);
  const snap = await userRef.get();
  const data = snap.data() || {};
  let newXp = (data.xp || 0) + amount;
  let newLevel = data.level || 1;
  // Thresholds: 1000 + (level-1)*500
  while (newXp >= 1000 + newLevel * 500) {
    newLevel++;
  }
  await userRef.update({ xp: newXp, level: newLevel });
  // If level up, create notification
  if (newLevel > (data.level || 1)) {
    await createNotification(uid, 'level_up', { level: newLevel });
    // Optional: push via FCM if token exists
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

// Helper: create a notification document
async function createNotification(uid, type, dataObj) {
  const nid = db.collection('notifications').doc(uid).collection('items').doc().id;
  await db.collection('notifications').doc(uid).collection('items').doc(nid).set({
    type,
    data: dataObj,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    read: false
  });
}

// API: awardXP (callable replacement)
export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const { uid, amount, reason } = req.body;
  if (!uid || amount == null) return res.status(400).json({ error: 'Missing uid or amount' });
  try {
    const result = await awardXP(uid, amount, reason);
    res.status(200).json(result);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
}

// Additional endpoints can be added as separate files in vercel/api/
// Example: vercel/api/follow.js, vercel/api/message.js, vercel/api/gameResult.js
