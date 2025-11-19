package com.WANGDULabs.VOXA.data.remote;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper to call Vercel serverless endpoints (replaces Firebase Functions without billing).
 * Ensure you set VERCEL_BASE_URL in your app (e.g., BuildConfig or a constant).
 */
public class VercelApi {
    private static final String TAG = "VercelApi";
    // Replace with your deployed Vercel URL
    private static final String BASE_URL = "https://your-vercel-app.vercel.app/api";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface VercelCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    // Acquire Firebase ID token for Authorization
    private static Task<String> getIdToken() {
        return Tasks.call(executor, () -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) throw new IllegalStateException("Not signed in");
            GetTokenResult result = Tasks.await(auth.getCurrentUser().getIdToken(false));
            return result.getToken();
        });
    }

    // Generic POST helper with Bearer token
    private static Task<JSONObject> post(String endpoint, JSONObject payload) {
        return getIdToken().continueWithTask(tokenTask -> {
            final String token = tokenTask.getResult();
            return Tasks.call(executor, () -> {
                try {
                    URL url = new URL(BASE_URL + endpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                    int responseCode = conn.getResponseCode();
                    InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }
                    if (responseCode >= 200 && responseCode < 300) {
                        return new JSONObject(response.toString());
                    } else {
                        throw new IOException("HTTP " + responseCode + ": " + response.toString());
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Vercel API error", e);
                    throw e;
                }
            });
        });
    }

    // Award XP
    public static Task<JSONObject> awardXP(int amount, String reason) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        JSONObject payload = new JSONObject();
        try {
            payload.put("uid", uid);
            payload.put("amount", amount);
            payload.put("reason", reason);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return post("", payload);
    }

    // Follow
    public static Task<JSONObject> follow(String otherUid) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        JSONObject payload = new JSONObject();
        try {
            payload.put("uid", uid);
            payload.put("other", otherUid);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return post("/follow", payload);
    }

    // Update conversation metadata on new message
    public static Task<JSONObject> onMessageSent(String conversationId, String senderId, String text, String imageUrl) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("cid", conversationId);
            payload.put("senderId", senderId);
            if (text != null) payload.put("text", text);
            if (imageUrl != null) payload.put("imageUrl", imageUrl);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return post("/message", payload);
    }

    // Log game result (XP + leaderboard)
    public static Task<JSONObject> logGameResult(String gameId, boolean won, int score) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        JSONObject payload = new JSONObject();
        try {
            payload.put("uid", uid);
            payload.put("gameId", gameId);
            payload.put("won", won);
            payload.put("score", score);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return post("/gameResult", payload);
    }

    // Example usage in an Activity
    public static void exampleAwardXPFromActivity() {
        awardXP(25, "win_rock_paper_scissors")
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "XP awarded: " + result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to award XP", e);
                });
    }

    // Multiplayer endpoints
    public static Task<JSONObject> createRoom(@NonNull String gameType) {
        JSONObject payload = new JSONObject();
        try { payload.put("gameType", gameType); } catch (JSONException e) { return Tasks.forException(e); }
        return post("/createRoom", payload);
    }

    public static Task<JSONObject> joinRoom(@NonNull String roomId) {
        JSONObject payload = new JSONObject();
        try { payload.put("roomId", roomId); } catch (JSONException e) { return Tasks.forException(e); }
        return post("/joinRoom", payload);
    }

    public static Task<JSONObject> submitMove(@NonNull String roomId, @NonNull JSONObject moveData) {
        try { moveData.put("roomId", roomId); } catch (JSONException e) { return Tasks.forException(e); }
        return post("/submitMove", moveData);
    }

    public static Task<JSONObject> finishGame(@NonNull String roomId, Integer score) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("roomId", roomId);
            if (score != null) payload.put("score", score);
        } catch (JSONException e) { return Tasks.forException(e); }
        return post("/finishGame", payload);
    }

    public static Task<JSONObject> exitRoom(@NonNull String roomId) {
        JSONObject payload = new JSONObject();
        try { payload.put("roomId", roomId); } catch (JSONException e) { return Tasks.forException(e); }
        return post("/exitRoom", payload);
    }
}
