// Vercel endpoint: /api/finishGame
// Finalizes game: stores result and marks room finished
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
  const { roomId, score } = req.body || {};
  if (!roomId) return res.status(400).json({ error: 'Missing roomId' });
  try {
    const roomRef = db.collection('rooms').doc(roomId);
    const snap = await roomRef.get();
    if (!snap.exists) return res.status(404).json({ error: 'Room not found' });
    const room = snap.data();
    if (![room.hostUid, room.opponentUid].includes(auth.uid)) return res.status(403).json({ error: 'Not a participant' });

    // If MathQuiz: ensure result exists (submitMove stores scores)
    const resultRef = roomRef.collection('result').doc('summary');
    const resultSnap = await resultRef.get();
    if (!resultSnap.exists && typeof score !== 'undefined') {
      // If only one score provided, write partial; winner decided when both present in submitMove
      const field = auth.uid === room.hostUid ? { hostScore: Number(score) } : { opponentScore: Number(score) };
      await resultRef.set(field, { merge: true });
    }
    await roomRef.update({ status: 'finished' });

    res.status(200).json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
}
