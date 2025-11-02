package com.WANGDULabs.VOXA.data.repository;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SocialRepository {
    public interface Callback { void onComplete(boolean success, String state, Exception e); }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore fs = FirebaseFirestore.getInstance();

    public void toggleFollowOrRequest(@NonNull String me, @NonNull String other, @NonNull Callback cb) {
        if (me.equals(other)) { cb.onComplete(false, "self", null); return; }
        fs.collection("users").document(other).get().addOnSuccessListener(userDoc -> {
            final boolean isPrivate = Boolean.TRUE.equals(userDoc.getBoolean("isPrivate"));
            DocumentReference followingDoc = fs.collection("follows").document(me).collection("following").document(other);
            followingDoc.get().addOnSuccessListener(fDoc -> {
                if (fDoc != null && fDoc.exists()) {
                    unfollow(me, other, cb);
                } else if (isPrivate) {
                    requestFollow(me, other, cb);
                } else {
                    follow(me, other, cb);
                }
            }).addOnFailureListener(e -> cb.onComplete(false, "error", e));
        }).addOnFailureListener(e -> cb.onComplete(false, "error", e));
    }

    public void follow(@NonNull String me, @NonNull String other, @NonNull Callback cb) {
        fs.runBatch(batch -> {
            Map<String, Object> meta = new HashMap<>();
            meta.put("createdAt", FieldValue.serverTimestamp());
            batch.set(fs.collection("follows").document(me).collection("following").document(other), meta);
            batch.set(fs.collection("follows").document(other).collection("followers").document(me), meta);
            batch.update(fs.collection("users").document(me), "followingCount", FieldValue.increment(1));
            batch.update(fs.collection("users").document(other), "followersCount", FieldValue.increment(1));
        }).addOnSuccessListener(v -> cb.onComplete(true, "following", null))
          .addOnFailureListener(e -> cb.onComplete(false, "error", e));
    }

    public void unfollow(@NonNull String me, @NonNull String other, @NonNull Callback cb) {
        fs.runBatch(batch -> {
            batch.delete(fs.collection("follows").document(me).collection("following").document(other));
            batch.delete(fs.collection("follows").document(other).collection("followers").document(me));
            batch.update(fs.collection("users").document(me), "followingCount", FieldValue.increment(-1));
            batch.update(fs.collection("users").document(other), "followersCount", FieldValue.increment(-1));
        }).addOnSuccessListener(v -> cb.onComplete(true, "unfollowed", null))
          .addOnFailureListener(e -> cb.onComplete(false, "error", e));
    }

    public void requestFollow(@NonNull String requester, @NonNull String target, @NonNull Callback cb) {
        fs.runBatch(batch -> {
            Map<String, Object> meta = new HashMap<>();
            meta.put("createdAt", FieldValue.serverTimestamp());
            batch.set(fs.collection("follow_requests").document(target).collection("incoming").document(requester), meta);
            batch.set(fs.collection("follow_requests").document(requester).collection("outgoing").document(target), meta);
        }).addOnSuccessListener(v -> cb.onComplete(true, "requested", null))
          .addOnFailureListener(e -> cb.onComplete(false, "error", e));
    }

    public void cancelRequest(@NonNull String requester, @NonNull String target, @NonNull Callback cb) {
        fs.runBatch(batch -> {
            batch.delete(fs.collection("follow_requests").document(target).collection("incoming").document(requester));
            batch.delete(fs.collection("follow_requests").document(requester).collection("outgoing").document(target));
        }).addOnSuccessListener(v -> cb.onComplete(true, "cancelled", null))
          .addOnFailureListener(e -> cb.onComplete(false, "error", e));
    }

    public void approveRequest(@NonNull String target, @NonNull String requester, @NonNull Callback cb) {
        // target approves requester
        fs.runBatch(batch -> {
            // create follow relationship
            Map<String, Object> meta = new HashMap<>();
            meta.put("createdAt", FieldValue.serverTimestamp());
            batch.set(fs.collection("follows").document(requester).collection("following").document(target), meta);
            batch.set(fs.collection("follows").document(target).collection("followers").document(requester), meta);
            batch.update(fs.collection("users").document(requester), "followingCount", FieldValue.increment(1));
            batch.update(fs.collection("users").document(target), "followersCount", FieldValue.increment(1));
            // remove requests
            batch.delete(fs.collection("follow_requests").document(target).collection("incoming").document(requester));
            batch.delete(fs.collection("follow_requests").document(requester).collection("outgoing").document(target));
        }).addOnSuccessListener(v -> cb.onComplete(true, "approved", null))
          .addOnFailureListener(e -> cb.onComplete(false, "error", e));
    }

    public void declineRequest(@NonNull String target, @NonNull String requester, @NonNull Callback cb) {
        fs.runBatch(batch -> {
            batch.delete(fs.collection("follow_requests").document(target).collection("incoming").document(requester));
            batch.delete(fs.collection("follow_requests").document(requester).collection("outgoing").document(target));
        }).addOnSuccessListener(v -> cb.onComplete(true, "declined", null))
          .addOnFailureListener(e -> cb.onComplete(false, "error", e));
    }
}
