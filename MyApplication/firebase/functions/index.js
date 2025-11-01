const functions = require('firebase-functions');
const admin = require('firebase-admin');
try { admin.initializeApp(); } catch (e) {}

exports.onFollowCreate = functions.firestore
  .document('follows/{uid}/following/{other}')
  .onCreate(async (snap, context) => {
    const { uid, other } = context.params;
    const db = admin.firestore();
    const batch = db.batch();
    const userRef = db.collection('users').doc(uid);
    const otherRef = db.collection('users').doc(other);
    batch.update(userRef, { followingCount: admin.firestore.FieldValue.increment(1) });
    batch.update(otherRef, { followersCount: admin.firestore.FieldValue.increment(1) });
    await batch.commit();
  });

exports.onFollowDelete = functions.firestore
  .document('follows/{uid}/following/{other}')
  .onDelete(async (snap, context) => {
    const { uid, other } = context.params;
    const db = admin.firestore();
    const batch = db.batch();
    const userRef = db.collection('users').doc(uid);
    const otherRef = db.collection('users').doc(other);
    batch.update(userRef, { followingCount: admin.firestore.FieldValue.increment(-1) });
    batch.update(otherRef, { followersCount: admin.firestore.FieldValue.increment(-1) });
    await batch.commit();
  });

exports.onMessageCreate = functions.firestore
  .document('conversations/{cid}/messages/{mid}')
  .onCreate(async (snap, context) => {
    const db = admin.firestore();
    const { cid } = context.params;
    const message = snap.data();
    const conv = (await db.collection('conversations').doc(cid).get()).data();
    if (!conv || !Array.isArray(conv.participants) || conv.participants.length !== 2) return;
    const senderId = message.senderId;
    const receiverId = conv.participants[0] === senderId ? conv.participants[1] : conv.participants[0];
    const senderDoc = await db.collection('users').doc(senderId).get();
    const receiverDoc = await db.collection('users').doc(receiverId).get();
    const token = receiverDoc.get('fcmToken');
    const senderName = senderDoc.get('displayName') || 'Someone';
    if (!token) return;
    const payload = {
      notification: {
        title: 'New message',
        body: `${senderName} sent you a message`,
      },
      data: {
        senderId: senderId,
        conversationId: cid,
      }
    };
    try { await admin.messaging().sendToDevice(token, payload); } catch (e) { console.error(e); }
  });

exports.onUserUpdatePropagate = functions.firestore
  .document('users/{uid}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const db = admin.firestore();
    const uid = context.params.uid;
    const changedName = before.displayName !== after.displayName;
    const changedPhoto = before.photoUrl !== after.photoUrl;
    if (!changedName && !changedPhoto) return;
    const authorUpdate = {};
    if (changedName) authorUpdate['author.displayName'] = after.displayName || '';
    if (changedPhoto) authorUpdate['author.photoUrl'] = after.photoUrl || '';
    // Example denormalization on posts and comments if present
    const postsSnap = await db.collection('posts').where('author.uid', '==', uid).get();
    const batch = db.batch();
    postsSnap.forEach(doc => batch.update(doc.ref, authorUpdate));
    const commentsSnap = await db.collection('comments').where('author.uid', '==', uid).get();
    commentsSnap.forEach(doc => batch.update(doc.ref, authorUpdate));
    await batch.commit();
  });
