package com.WANGDULabs.VOXA.data.multiplayer;

import androidx.annotation.NonNull;

import com.WANGDULabs.VOXA.data.remote.VercelApi;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

public class MoveManager {
    private final FirebaseFirestore fs = FirebaseFirestore.getInstance();
    private ListenerRegistration movesListener;

    public interface Callback<T> { void onResult(boolean ok, T value, Exception e); }

    public Task<org.json.JSONObject> submitMove(@NonNull String roomId, @NonNull JSONObject moveData) {
        return VercelApi.submitMove(roomId, moveData);
    }

    public void listenMoves(@NonNull String roomId, @NonNull EventListener<QuerySnapshot> listener) {
        stopListening();
        CollectionReference ref = fs.collection("rooms").document(roomId).collection("moves");
        movesListener = ref.addSnapshotListener(listener);
    }

    public void resetMoves(@NonNull String roomId) {
        // Optional: implemented on backend when a round finishes
    }

    public void stopListening() {
        if (movesListener != null) {
            movesListener.remove();
            movesListener = null;
        }
    }
}
