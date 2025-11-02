package com.WANGDULabs.VOXA.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.remote.VoxApi;
import com.WANGDULabs.VOXA.ui.adapters.PostsAdapter;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvDisplayName, tvLevel, tvXp, tvFollowers, tvFollowing;
    private MaterialButton btnEditProfile, btnNewPost;
    private RecyclerView recentFeed;
    private PostsAdapter postsAdapter;
    private List<JSONObject> posts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding to avoid status/nav bar overlap
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // Optionally adjust toolbar margin if present
            View toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                ((LinearLayout.LayoutParams) toolbar.getLayoutParams()).setMargins(0, systemBars.top, 0, 0);
            }
            return insets;
        });

        initViews();
        loadUserProfile();
        loadRecentFeed();
        setupClickListeners();
    }

    private void initViews() {
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvLevel = findViewById(R.id.tvLevel);
        tvXp = findViewById(R.id.tvXp);
        tvFollowers = findViewById(R.id.tvFollowers);
        tvFollowing = findViewById(R.id.tvFollowing);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnNewPost = findViewById(R.id.btnNewPost);
        recentFeed = findViewById(R.id.recentFeed);
        if (recentFeed != null) {
            recentFeed.setLayoutManager(new LinearLayoutManager(this));
            postsAdapter = new PostsAdapter(this, posts);
            recentFeed.setAdapter(postsAdapter);
        }
    }

    private void loadUserProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists()) {
                        tvDisplayName.setText(snap.getString("displayName"));
                        tvLevel.setText("Level " + snap.getLong("level"));
                        tvXp.setText(snap.getLong("xp") + " XP");
                        tvFollowers.setText("Followers: " + snap.getLong("followersCount"));
                        tvFollowing.setText("Following: " + snap.getLong("followingCount"));
                    }
                })
                .addOnFailureListener(e -> {
                    // Optionally show error
                });
    }

    private void loadRecentFeed() {
        VoxApi.fetchFeed(10, null)
                .addOnSuccessListener(result -> {
                    try {
                        posts.clear();
                        org.json.JSONArray arr = result.getJSONArray("posts");
                        for (int i = 0; i < arr.length(); i++) {
                            posts.add(arr.getJSONObject(i));
                        }
                        postsAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        // Handle error
                    }
                })
                .addOnFailureListener(e -> {
                    // Optionally show error
                });
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
        btnNewPost.setOnClickListener(v -> {
            // TODO: Open post creation UI
        });
    }
}
