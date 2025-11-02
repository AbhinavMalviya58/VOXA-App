// Vercel endpoint: /api/gameResult
// Handles XP award and leaderboard updates on game completion (replaces logGameResult callable)
const admin = require('firebase-admin');
if (!admin.apps.length) {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: `https://${process.env.FIREBASE_PROJECT_ID}-default-rtdb.firebaseio.com`
  });
}
const db = admin.firestore();

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
  // If level up, create notification
  if (newLevel > (data.level || 1)) {
    await db.collection('notifications').doc(uid).collection('items').doc().set({
      type: 'level_up',
      data: { level: newLevel },
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      read: false
    });
  }
  return { xp: newXp, level: newLevel };
}

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const { uid, gameId, won, score } = req.body;
  if (!uid || !gameId) return res.status(400).json({ error: 'Missing uid or gameId' });
  try {
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
    const xpResult = await awardXP(uid, xpAmount, `game_${gameId}`);
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
    res.status(200).json({ success: true, xp: xpResult });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
}
