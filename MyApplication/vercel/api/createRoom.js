// Vercel endpoint: /api/createRoom
// Creates a multiplayer room with a 6-digit code and initializes per-game server data.
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

function generateRoomCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

function generateMathQuizQuestions(n = 10) {
  const ops = ['+', '-', '*'];
  const qs = [];
  for (let i = 0; i < n; i++) {
    const a = Math.floor(Math.random() * 20) + 1;
    const b = Math.floor(Math.random() * 20) + 1;
    const op = ops[Math.floor(Math.random() * ops.length)];
    let answer = 0;
    if (op === '+') answer = a + b;
    else if (op === '-') answer = a - b;
    else answer = a * b;
    qs.push({ a, b, op, answer });
  }
  return qs;
}

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const auth = await requireAuth(req, res);
  if (!auth) return;
  const { gameType } = req.body || {};
  if (!gameType) return res.status(400).json({ error: 'Missing gameType' });
  try {
    // Generate unique 6-digit room code
    let roomId = null; let attempts = 0;
    while (attempts < 5) {
      attempts++;
      const code = generateRoomCode();
      const ref = db.collection('rooms').doc(code);
      if (!(await ref.get()).exists) { roomId = code; break; }
    }
    if (!roomId) return res.status(503).json({ error: 'Try again' });

    const roomRef = db.collection('rooms').doc(roomId);
    await roomRef.set({
      gameType,
      hostUid: auth.uid,
      opponentUid: null,
      status: 'waiting',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Per-game server data
    if (gameType === 'MathQuiz') {
      await roomRef.collection('state').doc('quiz').set({
        questions: generateMathQuizQuestions(10),
        durationMs: 60000,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } else if (gameType === 'GuessNumber') {
      const target = Math.floor(Math.random() * 101);
      await roomRef.collection('server').doc('secret').set({ target });
    } else if (gameType === 'TicTacToe') {
      await roomRef.collection('state').doc('board').set({
        board: ['', '', '', '', '', '', '', '', ''],
        turn: auth.uid,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    res.status(200).json({ roomId });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
}
