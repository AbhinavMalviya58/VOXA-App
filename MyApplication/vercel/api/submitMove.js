// Vercel endpoint: /api/submitMove
// Stores player move and resolves results for supported games when both moves received
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

function rpsWinner(a, b) {
  if (a === b) return 0;
  if ((a === 'rock' && b === 'scissors') || (a === 'paper' && b === 'rock') || (a === 'scissors' && b === 'paper')) return 1;
  return -1;
}

function evenOddWinner(m1, m2) {
  const sum = (m1.number|0) + (m2.number|0);
  const isEven = (sum % 2) === 0;
  const p1Wins = (m1.choice === 'even' && isEven) || (m1.choice === 'odd' && !isEven);
  const p2Wins = (m2.choice === 'even' && isEven) || (m2.choice === 'odd' && !isEven);
  if (p1Wins && !p2Wins) return 1;
  if (p2Wins && !p1Wins) return -1;
  return 0; // tie
}

function tttCheck(boardArr) {
  const lines = [
    [0,1,2],[3,4,5],[6,7,8],
    [0,3,6],[1,4,7],[2,5,8],
    [0,4,8],[2,4,6]
  ];
  for (const [a,b,c] of lines) {
    if (boardArr[a] && boardArr[a] === boardArr[b] && boardArr[a] === boardArr[c]) {
      return boardArr[a]; // 'X' or 'O'
    }
  }
  if (boardArr.every(v => v)) return 'draw';
  return null;
}

export default async function handler(req, res) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const auth = await requireAuth(req, res);
  if (!auth) return;
  const { roomId, move, choice, number, guess, answers, score, cellIndex } = req.body || {};
  if (!roomId) return res.status(400).json({ error: 'Missing roomId' });
  try {
    const roomRef = db.collection('rooms').doc(roomId);
    const roomSnap = await roomRef.get();
    if (!roomSnap.exists) return res.status(404).json({ error: 'Room not found' });
    const room = roomSnap.data();
    if (room.status !== 'playing') return res.status(400).json({ error: 'Room not playing' });
    if (![room.hostUid, room.opponentUid].includes(auth.uid)) return res.status(403).json({ error: 'Not a participant' });

    // Write move doc
    const moveDoc = { updatedAt: admin.firestore.FieldValue.serverTimestamp() };
    if (move) moveDoc.move = String(move);
    if (choice) moveDoc.choice = String(choice);
    if (typeof number !== 'undefined') moveDoc.number = Number(number);
    if (typeof guess !== 'undefined') moveDoc.guess = Number(guess);
    if (Array.isArray(answers)) moveDoc.answers = answers;
    if (typeof score !== 'undefined') moveDoc.score = Number(score);
    await roomRef.collection('moves').doc(auth.uid).set(moveDoc, { merge: true });

    // Resolve results for certain games
    const hostUid = room.hostUid; const oppUid = room.opponentUid;
    const hostMove = (await roomRef.collection('moves').doc(hostUid).get()).data() || {};
    const oppMove = (await roomRef.collection('moves').doc(oppUid).get()).data() || {};

    const resultRef = roomRef.collection('result').doc('summary');
    const gameType = room.gameType;

    if (gameType === 'RPS') {
      if (hostMove.move && oppMove.move) {
        const w = rpsWinner(hostMove.move, oppMove.move);
        let winnerUid = null; if (w === 1) winnerUid = hostUid; else if (w === -1) winnerUid = oppUid;
        await resultRef.set({ winnerUid, hostScore: w === 1 ? 1 : 0, opponentScore: w === -1 ? 1 : 0 }, { merge: true });
      }
    } else if (gameType === 'EvenOdd') {
      if (hostMove.choice && typeof hostMove.number !== 'undefined' && oppMove.choice && typeof oppMove.number !== 'undefined') {
        const w = evenOddWinner(hostMove, oppMove);
        let winnerUid = null; if (w === 1) winnerUid = hostUid; else if (w === -1) winnerUid = oppUid;
        await resultRef.set({ winnerUid, hostScore: w === 1 ? 1 : 0, opponentScore: w === -1 ? 1 : 0 }, { merge: true });
      }
    } else if (gameType === 'GuessNumber') {
      if (typeof hostMove.guess !== 'undefined' || typeof oppMove.guess !== 'undefined') {
        const secretSnap = await roomRef.collection('server').doc('secret').get();
        const target = secretSnap.exists ? secretSnap.data().target : null;
        if (target != null) {
          let winnerUid = null;
          if (hostMove.guess === target) winnerUid = hostUid;
          if (oppMove.guess === target) winnerUid = winnerUid ? winnerUid : oppUid; // first correct wins; we can't order without timestamps, so pick first present
          if (winnerUid && !(await resultRef.get()).exists) await resultRef.set({ winnerUid, hostScore: hostMove.guess === target ? 1 : 0, opponentScore: oppMove.guess === target ? 1 : 0 }, { merge: true });
        }
      }
    } else if (gameType === 'MathQuiz') {
      // Compute score from provided answers against server-stored questions
      const quizSnap = await roomRef.collection('state').doc('quiz').get();
      if (quizSnap.exists && Array.isArray(answers)) {
        const qs = quizSnap.data().questions || [];
        let correct = 0;
        for (let i = 0; i < Math.min(qs.length, answers.length); i++) {
          if (Number(answers[i]) === Number(qs[i].answer)) correct++;
        }
        await roomRef.collection('moves').doc(auth.uid).set({ score: correct }, { merge: true });
      }
      const hostScore = (await roomRef.collection('moves').doc(hostUid).get()).data()?.score;
      const oppScore = (await roomRef.collection('moves').doc(oppUid).get()).data()?.score;
      if (typeof hostScore !== 'undefined' && typeof oppScore !== 'undefined') {
        let winnerUid = null;
        if (hostScore > oppScore) winnerUid = hostUid; else if (oppScore > hostScore) winnerUid = oppUid;
        await resultRef.set({ winnerUid, hostScore: hostScore|0, opponentScore: oppScore|0 }, { merge: true });
      }
    } else if (gameType === 'TicTacToe') {
      // cellIndex is required; host is 'X', opponent is 'O'
      if (typeof cellIndex === 'number') {
        const boardRef = roomRef.collection('state').doc('board');
        const boardSnap = await boardRef.get();
        const data = boardSnap.data() || { board: ['', '', '', '', '', '', '', '', ''], turn: room.hostUid };
        if (![0,1,2,3,4,5,6,7,8].includes(cellIndex)) throw new Error('Bad index');
        if (data.board[cellIndex]) return res.status(200).json({ success: true }); // already occupied
        const mySymbol = auth.uid === room.hostUid ? 'X' : 'O';
        if (data.turn !== auth.uid) return res.status(400).json({ error: 'Not your turn' });
        data.board[cellIndex] = mySymbol;
        const nextTurn = auth.uid === room.hostUid ? room.opponentUid : room.hostUid;
        await boardRef.set({ board: data.board, turn: nextTurn, updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
        const outcome = tttCheck(data.board);
        if (outcome) {
          let winnerUid = null;
          if (outcome === 'X') winnerUid = room.hostUid; else if (outcome === 'O') winnerUid = room.opponentUid;
          await resultRef.set({ winnerUid, hostScore: outcome === 'X' ? 1 : 0, opponentScore: outcome === 'O' ? 1 : 0 }, { merge: true });
        }
      }
    }

    res.status(200).json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
}
