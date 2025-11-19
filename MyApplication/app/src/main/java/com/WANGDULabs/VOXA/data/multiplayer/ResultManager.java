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

public class ResultManager {
    private final FirebaseFirestore fs = FirebaseFirestore.getInstance();
    private ListenerRegistration resultListener;

    public interface Callback<T> { void onResult(boolean ok, T value, Exception e); }

    public void listenResult(@NonNull String roomId, @NonNull EventListener<DocumentSnapshot> listener) {
        stopListening();
        DocumentReference ref = fs.collection("rooms").document(roomId).collection("result").document("summary");
        resultListener = ref.addSnapshotListener(listener);
    }

    public Task<org.json.JSONObject> pushResult(@NonNull String roomId, Integer score) {
        return VercelApi.finishGame(roomId, score);
    }

    public void stopListening() {
        if (resultListener != null) {
            resultListener.remove();
            resultListener = null;
        }
    }
}
