// Vercel endpoint: /api/follow
// Handles follow creation and counter updates (replaces onFollowCreate Cloud Function)
const admin = require('firebase-admin');
// Ensure admin is initialized (shared from index.js or reinitialize)
if (!admin.apps.length) {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: `https://${process.env.FIREBASE_PROJECT_ID}-default-rtdb.firebaseio.com`
  });
}
const db = admin.firestore();

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const { uid, other } = req.body;
  if (!uid || !other) return res.status(400).json({ error: 'Missing uid or other' });
  try {
    const batch = db.batch();
    const userRef = db.collection('users').doc(uid);
    const otherRef = db.collection('users').doc(other);
    batch.update(userRef, { followingCount: admin.firestore.FieldValue.increment(1) });
    batch.update(otherRef, { followersCount: admin.firestore.FieldValue.increment(1) });
    await batch.commit();
    // Optional: create notification for target
    await db.collection('notifications').doc(other).collection('items').doc().set({
      type: 'follow',
      data: { followerUid: uid },
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      read: false
    });
    res.status(200).json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
}
