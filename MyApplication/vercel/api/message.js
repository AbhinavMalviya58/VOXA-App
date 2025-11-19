// Vercel endpoint: /api/message
// Handles conversation metadata update on new message (replaces onMessageCreate Cloud Function)

const admin = require('firebase-admin');

if (!admin.apps.length) {
  try {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: `https://${process.env.FIREBASE_PROJECT_ID}-default-rtdb.firebaseio.com`
    });
  } catch (err) {
    console.error("Firebase initialization error:", err);
  }
}

const db = admin.firestore();
const fcm = admin.messaging();

module.exports = async (req, res) => {
  if (req.method !== 'POST')
    return res.status(405).json({ error: 'Method not allowed' });

  const { cid, senderId, text, imageUrl } = req.body;
  if (!cid || !senderId)
    return res.status(400).json({ error: 'Missing cid or senderId' });

  try {
    // Update conversation metadata
    const convRef = db.collection('conversations').doc(cid);
    const convSnap = await convRef.get();

    if (!convSnap.exists)
      return res.status(404).json({ error: 'Conversation not found' });

    const conv = convSnap.data();
    const participants = conv.participants || [];
    const receiverId = participants.find(p => p !== senderId);
    const lastMessage = text || (imageUrl ? '📷 Photo' : '');

    await convRef.set({
      participants,
      lastMessage,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Send push notification if receiver has FCM token
    if (receiverId) {
      const receiverDoc = await db.collection('users').doc(receiverId).get();
      const token = receiverDoc.get('fcmToken');

      const senderDoc = await db.collection('users').doc(senderId).get();
      const senderName = senderDoc.get('displayName') || 'Someone';

      if (token) {
        await fcm.sendToDevice(token, {
          notification: {
            title: 'New message',
            body: `${senderName}: ${lastMessage}`
          },
          data: {
            senderId,
            conversationId: cid
          }
        });
      }
    }

    return res.status(200).json({ success: true });

  } catch (e) {
    console.error("Message API error:", e);
    return res.status(500).json({ error: e.message });
  }
};
