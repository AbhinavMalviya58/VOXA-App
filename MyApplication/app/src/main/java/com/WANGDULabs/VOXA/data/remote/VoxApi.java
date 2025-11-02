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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * API client for VOXA Express backend (Vercel/Render).
 * Uses Firebase ID token for Authorization.
 */
public class VoxApi {
    private static final String TAG = "VoxApi";
    // Replace with your deployed backend URL (Vercel or Render)
    private static final String BASE_URL = "https://your-backend.vercel.app";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface VoxCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    private static Task<String> getIdToken() {
        return Tasks.call(executor, () -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) throw new IllegalStateException("Not signed in");
            GetTokenResult result = Tasks.await(auth.getCurrentUser().getIdToken(false));
            return result.getToken();
        });
    }

    private static Task<JSONObject> request(String method, String endpoint, JSONObject payload) {
        return getIdToken().continueWithTask(tokenTask -> {
            String token = tokenTask.getResult();
            return Tasks.call(executor, () -> {
                try {
                    URL url = new URL(BASE_URL + endpoint);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    if (!method.equals("GET") && payload != null) {
                        conn.setDoOutput(true);
                        try (OutputStream os = conn.getOutputStream()) {
                            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                            os.write(input, 0, input.length);
                        }
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
                    Log.e(TAG, "API error", e);
                    throw e;
                }
            });
        });
    }

    // Follow
    public static Task<JSONObject> follow(String otherUid) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("other", otherUid);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return request("POST", "/follow", payload);
    }

    // Unfollow
    public static Task<JSONObject> unfollow(String otherUid) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("other", otherUid);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return request("POST", "/unfollow", payload);
    }

    // Award XP
    public static Task<JSONObject> awardXP(int amount, String reason) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("amount", amount);
            payload.put("reason", reason);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return request("POST", "/awardXP", payload);
    }

    // Game result
    public static Task<JSONObject> logGameResult(String gameId, boolean won, int score) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("gameId", gameId);
            payload.put("won", won);
            payload.put("score", score);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return request("POST", "/gameResult", payload);
    }

    // Message metadata update
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
        return request("POST", "/message", payload);
    }

    // Create post (no file upload for now; use separate endpoint for media)
    public static Task<JSONObject> createPost(String text) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("text", text);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return request("POST", "/post", payload);
    }

    // Like post
    public static Task<JSONObject> likePost(String postId) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("postId", postId);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return request("POST", "/like", payload);
    }

    // Add comment
    public static Task<JSONObject> addComment(String postId, String text) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("postId", postId);
            payload.put("text", text);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return request("POST", "/comment", payload);
    }

    // Fetch feed
    public static Task<JSONObject> fetchFeed(int limit, String startAfter) {
        String qs = "?limit=" + limit;
        if (startAfter != null) qs += "&startAfter=" + startAfter;
        return request("GET", "/feed" + qs, null);
    }

    // Fetch notifications
    public static Task<JSONObject> fetchNotifications(int limit) {
        return request("GET", "/notifications?limit=" + limit, null);
    }

    // Update profile
    public static Task<JSONObject> updateProfile(String displayName, String bio, boolean isPrivate) {
        JSONObject payload = new JSONObject();
        try {
            if (displayName != null) payload.put("displayName", displayName);
            if (bio != null) payload.put("bio", bio);
            payload.put("isPrivate", isPrivate);
        } catch (JSONException e) {
            return Tasks.forException(e);
        }
        return request("POST", "/profile", payload);
    }

    // Health check
    public static Task<JSONObject> health() {
        return request("GET", "/health", null);
    }
}
