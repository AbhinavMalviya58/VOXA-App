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

async function requireAuth(req, res) {
  const token = req.headers.authorization?.split('Bearer ')[1];
  if (!token) { res.status(401).json({ error: 'Unauthorized' }); return null; }
  try { return await admin.auth().verifyIdToken(token); } catch (e) { res.status(401).json({ error: 'Invalid token' }); return null; }
}

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const auth = await requireAuth(req, res);
  if (!auth) return;
  const { other } = req.body;
  const uid = auth.uid;
  if (!other) return res.status(400).json({ error: 'Missing other' });
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
