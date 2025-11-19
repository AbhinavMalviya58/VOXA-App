// Vercel endpoint: /api/joinRoom
// Joins an existing room by 6-digit code and moves status to 'playing'
const admin = require('firebase-admin');
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
  if (!token) {
    res.status(401).json({ error: 'Unauthorized' });
    return null;
  }
  try {
    const decoded = await admin.auth().verifyIdToken(token);
    return decoded;
  } catch (e) {
    res.status(401).json({ error: 'Invalid token' });
    return null;
  }
}

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const auth = await requireAuth(req, res);
  if (!auth) return;
  const { roomId } = req.body || {};
  if (!roomId) return res.status(400).json({ error: 'Missing roomId' });
  try {
    const roomRef = db.collection('rooms').doc(roomId);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(roomRef);
      if (!snap.exists) throw new Error('Room not found');
      const room = snap.data();
      if (room.status !== 'waiting') throw new Error('Room not joinable');
      if (room.hostUid === auth.uid) throw new Error('Host cannot join own room');
      if (room.opponentUid && room.opponentUid !== auth.uid) throw new Error('Room full');
      tx.update(roomRef, { opponentUid: auth.uid, status: 'playing' });
    });
    res.status(200).json({ success: true });
  } catch (e) {
    res.status(400).json({ error: e.message });
  }
}
