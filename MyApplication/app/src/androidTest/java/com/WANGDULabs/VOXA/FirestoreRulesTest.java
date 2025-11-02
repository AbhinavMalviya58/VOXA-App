package com.WANGDULabs.VOXA;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FirestoreRulesTest {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Before
    public void setUp() throws Exception {
        // Initialize Firebase with emulator
        FirebaseApp.clearInstancesForTest();
        FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext());
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Point to emulator
        db.useEmulator("10.0.2.2", 8080);
        auth.useEmulator("10.0.2.2", 9099);
    }

    @After
    public void tearDown() throws Exception {
        // Sign out after each test
        auth.signOut();
    }

    @Test
    public void testPublicReadUsers() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        db.collection("users").limit(1).get()
                .addOnSuccessListener(snap -> {
                    assertTrue("Public users should be readable", !snap.isEmpty());
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    fail("Public read of users failed: " + e.getMessage());
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testSelfCanWriteOwnProfile() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        // Sign in as a test user (you must create this user in the emulator auth beforehand)
        auth.signInAnonymously()
                .continueWithTask(task -> {
                    String uid = task.getResult().getUser().getUid();
                    Map<String, Object> data = new HashMap<>();
                    data.put("displayName", "TestUser");
                    data.put("bio", "Test bio");
                    return db.collection("users").document(uid).set(data);
                })
                .addOnSuccessListener(aVoid -> {
                    assertTrue("User should be able to write own profile", true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    fail("Self write to profile failed: " + e.getMessage());
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testCannotWriteXpDirectly() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        auth.signInAnonymously()
                .continueWithTask(task -> {
                    String uid = task.getResult().getUser().getUid();
                    Map<String, Object> data = new HashMap<>();
                    data.put("xp", 5000);
                    return db.collection("users").document(uid).set(data);
                })
                .addOnSuccessListener(aVoid -> {
                    fail("Client should NOT be able to write xp directly");
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    assertTrue("Write xp should be denied", true);
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testFollowsReadWrite() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        auth.signInAnonymously()
                .continueWithTask(task -> {
                    String uid = task.getResult().getUser().getUid();
                    // Create a follow relationship
                    Map<String, Object> data = new HashMap<>();
                    data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    return db.collection("follows").document(uid).collection("following").document("otherUser123").set(data);
                })
                .addOnSuccessListener(aVoid -> {
                    assertTrue("User should be able to write their own following", true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    fail("Follow write failed: " + e.getMessage());
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testFollowRequestsCreateAndDelete() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        auth.signInAnonymously()
                .continueWithTask(task -> {
                    String uid = task.getResult().getUser().getUid();
                    Map<String, Object> data = new HashMap<>();
                    data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    // Create outgoing request
                    return db.collection("follow_requests").document(uid).collection("outgoing").document("targetUser456").set(data);
                })
                .continueWithTask(task -> {
                    // Now delete it (simulate approve/decline)
                    String uid = auth.getCurrentUser().getUid();
                    return db.collection("follow_requests").document(uid).collection("outgoing").document("targetUser456").delete();
                })
                .addOnSuccessListener(aVoid -> {
                    assertTrue("Follow request create/delete should succeed", true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    fail("Follow request operation failed: " + e.getMessage());
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testConversationMetadataUpdateAllowed() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        auth.signInAnonymously()
                .continueWithTask(task -> {
                    String uid = task.getResult().getUser().getUid();
                    // Create a conversation with self as a participant
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("participants", java.util.Arrays.asList(uid, "otherUser789"));
                    conv.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    conv.put("lastMessage", "Hello");
                    return db.collection("conversations").document("conv123").set(conv);
                })
                .addOnSuccessListener(aVoid -> {
                    assertTrue("Conversation metadata write should succeed for participants", true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    fail("Conversation metadata write failed: " + e.getMessage());
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testMessageCreateOnlyIfMutualFollow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        auth.signInAnonymously()
                .continueWithTask(task -> {
                    String uid = task.getResult().getUser().getUid();
                    // Ensure mutual follow exists (you must pre-populate follows in emulator)
                    // For this test, we assume mutual follow is NOT set, so it should fail
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("senderId", uid);
                    msg.put("text", "Test");
                    msg.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    return db.collection("conversations").document("conv123").collection("messages").document("msg1").set(msg);
                })
                .addOnSuccessListener(aVoid -> {
                    fail("Message create should fail without mutual follow");
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    assertTrue("Message create should be denied without mutual follow", true);
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testPostsCreateAndLike() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        auth.signInAnonymously()
                .continueWithTask(task -> {
                    String uid = task.getResult().getUser().getUid();
                    // Create a post
                    Map<String, Object> post = new HashMap<>();
                    post.put("author", Map.of("uid", uid, "displayName", "TestUser", "photoUrl", ""));
                    post.put("text", "Hello VOXA");
                    post.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    return db.collection("posts").document("post123").set(post);
                })
                .continueWithTask(task -> {
                    String uid = auth.getCurrentUser().getUid();
                    // Like the post
                    Map<String, Object> like = new HashMap<>();
                    like.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    return db.collection("posts").document("post123").collection("likes").document(uid).set(like);
                })
                .addOnSuccessListener(aVoid -> {
                    assertTrue("Post create and like should succeed", true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    fail("Post/like operation failed: " + e.getMessage());
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testVibeReadonly() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        db.collection("vibe").limit(1).get()
                .addOnSuccessListener(snap -> {
                    assertTrue("Vibe collection should be publicly readable", true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    fail("Vibe read failed: " + e.getMessage());
                    latch.countDown();
                });
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
    }
}
