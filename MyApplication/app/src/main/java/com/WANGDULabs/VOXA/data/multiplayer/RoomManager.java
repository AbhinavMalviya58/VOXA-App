package com.WANGDULabs.VOXA.data.multiplayer;

import androidx.annotation.NonNull;

import com.WANGDULabs.VOXA.data.remote.VercelApi;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RoomManager {
    private final FirebaseFirestore fs = FirebaseFirestore.getInstance();
    private ListenerRegistration roomListener;

    public interface Callback<T> { void onResult(boolean ok, T value, Exception e); }

    public void createRoom(@NonNull String gameType, @NonNull Callback<String> cb) {
        VercelApi.createRoom(gameType)
                .addOnSuccessListener(json -> {
                    String roomId = json.optString("roomId", null);
                    cb.onResult(roomId != null, roomId, null);
                })
                .addOnFailureListener(e -> cb.onResult(false, null, e));
    }

    public void joinRoom(@NonNull String roomId, @NonNull Callback<Boolean> cb) {
        VercelApi.joinRoom(roomId)
                .addOnSuccessListener(json -> cb.onResult(true, true, null))
                .addOnFailureListener(e -> cb.onResult(false, false, e));
    }

    public void leaveRoom(@NonNull String roomId, @NonNull Callback<Boolean> cb) {
        VercelApi.exitRoom(roomId)
                .addOnSuccessListener(json -> cb.onResult(true, true, null))
                .addOnFailureListener(e -> cb.onResult(false, false, e));
    }

    public void listenRoom(@NonNull String roomId, @NonNull EventListener<DocumentSnapshot> listener) {
        stopListening();
        DocumentReference ref = fs.collection("rooms").document(roomId);
        roomListener = ref.addSnapshotListener(listener);
    }

    public void stopListening() {
        if (roomListener != null) {
            roomListener.remove();
            roomListener = null;
        }
    }
}
