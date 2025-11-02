package com.WANGDULabs.VOXA;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.WANGDULabs.VOXA.ui.activities.loginTab;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;
import com.WANGDULabs.VOXA.data.repository.SocialRepository;

import java.util.Objects;

public class profilePage extends AppCompatActivity {

    // UI Views
    private ImageView userImg, profileBanner;
    private TextView shoEmail, user, genderName, dob, profileName;
    private TextView xpValue, levelValue, totalWinsValue, currentStreakValue, highestStreakValue, rankValue;
    private TextView bioValue, textUserId, followersCount, followingCount;
    private ProgressBar xpProgressBar;
    private Button logout_button, btnFollow, btnShare;
    private FloatingActionButton fabEdit, fabChangePhoto;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseFirestore firestore;
    private ListenerRegistration userDocListener;

    // Variables
    private String profileUid;
    private boolean isSelf = true;
    private String followState = ""; // "following", "requested", "none"
    private final SocialRepository socialRepo = new SocialRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        // Check if user is logged in
        if (mUser == null) {
            goToLogin();
            return;
        }

        initializeViews();
        setupToolbar();

        // Determine which profile to show (self or other user)
        String extraUid = getIntent().getStringExtra("uid");
        profileUid = (extraUid != null && !extraUid.isEmpty()) ? extraUid : mUser.getUid();
        isSelf = profileUid.equals(mUser.getUid());

        configureUiBasedOnProfileType();
        setupListeners();
        loadDataFromFirestore();

        com.WANGDULabs.VOXA.ui.navigation.FooterController.bind(this, FooterController.Tab.HOME);
        if (!isSelf) refreshFollowState();
    }

    /**
     * Finds and assigns all the UI elements from the XML layout.
     */
    private void initializeViews() {
        userImg = findViewById(R.id.userImg);
        profileBanner = findViewById(R.id.profile_banner);
        shoEmail = findViewById(R.id.shoEmail);
        user = findViewById(R.id.user);
        logout_button = findViewById(R.id.logout_button);
        genderName = findViewById(R.id.genderName);
        dob = findViewById(R.id.DOB);
        // profileName = findViewById(R.id.profile); // Assuming this is part of toolbar now

        xpValue = findViewById(R.id.xpValue);
        levelValue = findViewById(R.id.levelValue);
        totalWinsValue = findViewById(R.id.totalWinsValue);
        currentStreakValue = findViewById(R.id.currentStreakValue);
        highestStreakValue = findViewById(R.id.highestStreakValue);
        rankValue = findViewById(R.id.rankValue);
        xpProgressBar = findViewById(R.id.xpProgressBar);

        bioValue = findViewById(R.id.bioValue);
        textUserId = findViewById(R.id.textUserId);
        followersCount = findViewById(R.id.followersCount);
        followingCount = findViewById(R.id.followingCount);

        btnFollow = findViewById(R.id.btnFollow);
        btnShare = findViewById(R.id.btnShare);
        fabEdit = findViewById(R.id.fabEdit);
        fabChangePhoto = findViewById(R.id.fabChangePhoto);
    }

    /**
     * Sets up the toolbar with a back button.
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.materialToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true); // Can set title here
        }
    }

    /**
     * Hides or shows buttons based on whether the user is viewing their own profile.
     */
    private void configureUiBasedOnProfileType() {
        if (isSelf) {
            btnFollow.setVisibility(View.GONE);
            fabEdit.setVisibility(View.VISIBLE);
            logout_button.setVisibility(View.VISIBLE);
        } else {
            btnFollow.setVisibility(View.VISIBLE);
            fabEdit.setVisibility(View.GONE);
            logout_button.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up OnClickListeners for all interactive UI elements.
     */
    private void setupListeners() {
        logout_button.setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });

        fabEdit.setOnClickListener(v -> {
            // TODO: Implement edit mode logic
            Toast.makeText(this, "Edit Profile Clicked!", Toast.LENGTH_SHORT).show();
        });

        btnFollow.setOnClickListener(v -> onFollowClicked());
    }

    /**
     * Attaches a real-time listener to the user's Firestore document to fetch and display data.
     */
    private void loadDataFromFirestore() {
        if (profileUid == null) return;

        DocumentReference userRef = firestore.collection("users").document(profileUid);
        userDocListener = userRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                // Handle error
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                populateUiWithData(snapshot);
            } else {
                // Handle case where user document doesn't exist
            }
        });
    }

    /**
     * Populates all the UI views with data from the Firestore snapshot.
     * @param snapshot The DocumentSnapshot containing user data.
     */
    private void populateUiWithData(DocumentSnapshot snapshot) {
        // --- Profile Header Data ---
        user.setText(snapshot.getString("displayName"));
        textUserId.setText(String.format("@%s", snapshot.getString("userId_lc")));
        bioValue.setText(snapshot.getString("bio"));

        Glide.with(this)
                .load(snapshot.getString("photoUrl"))
                .placeholder(R.drawable.default_avatar) // Add a default avatar
                .circleCrop()
                .into(userImg);

        Glide.with(this)
                .load(snapshot.getString("bannerUrl"))
                .placeholder(R.drawable.default_banner) // Add a default banner
                .into(profileBanner);

        // --- Followers/Following ---
        followersCount.setText(String.valueOf(snapshot.getLong("followersCount")));
        followingCount.setText(String.valueOf(snapshot.getLong("followingCount")));

        // --- Game Stats ---
        long level = snapshot.getLong("level") != null ? snapshot.getLong("level") : 1;
        long currentXp = snapshot.getLong("xp") != null ? snapshot.getLong("xp") : 0;
        long xpForNextLevel = calculateXpForNextLevel(level); // Dummy function

        levelValue.setText("Level: " + level);
        xpValue.setText(String.format("%d / %d XP", currentXp, xpForNextLevel));
        xpProgressBar.setMax((int)xpForNextLevel);
        xpProgressBar.setProgress((int)currentXp);

        rankValue.setText("#" + snapshot.getLong("rank"));
        totalWinsValue.setText(String.valueOf(snapshot.getLong("totalWins")));
        highestStreakValue.setText(String.valueOf(snapshot.getLong("highestStreak")));
        currentStreakValue.setText(String.valueOf(snapshot.getLong("currentStreak")));

        // --- User Info ---
        shoEmail.setText(snapshot.getString("email"));
        genderName.setText(snapshot.getString("gender"));
        dob.setText(snapshot.getString("dateOfBirth"));

        if (!isSelf) refreshFollowState();
    }

    /**
     * A placeholder function to calculate XP required for the next level.
     * Replace this with your actual game logic.
     */
    private long calculateXpForNextLevel(long currentLevel) {
        return currentLevel * 1000; // Example: Level 1 needs 1000 XP, Level 2 needs 2000 XP, etc.
    }

    private void refreshFollowState() {
        if (mUser == null || profileUid == null) return;
        String me = mUser.getUid(); String other = profileUid;
        FirebaseFirestore.getInstance().collection("follows").document(me).collection("following").document(other).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        setFollowState("following");
                    } else {
                        FirebaseFirestore.getInstance().collection("follow_requests").document(me)
                                .collection("outgoing").document(other).get().addOnSuccessListener(req -> {
                                    if (req != null && req.exists()) setFollowState("requested");
                                    else setFollowState("none");
                                });
                    }
                });
    }

    private void setFollowState(String state) {
        followState = state;
        if ("following".equals(state)) {
            btnFollow.setText("Following");
        } else if ("requested".equals(state)) {
            btnFollow.setText("Requested");
        } else {
            btnFollow.setText("Follow");
        }
    }

    private void onFollowClicked() {
        if (mUser == null || isSelf || profileUid == null) return;
        String me = mUser.getUid(); String other = profileUid;
        switch (followState) {
            case "following":
                socialRepo.unfollow(me, other, (success, state, e) -> runOnUiThread(this::refreshFollowState));
                break;
            case "requested":
                socialRepo.cancelRequest(me, other, (success, state, e) -> runOnUiThread(this::refreshFollowState));
                break;
            default:
                socialRepo.toggleFollowOrRequest(me, other, (success, state, e) -> runOnUiThread(this::refreshFollowState));
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(getApplicationContext(), loginTab.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the listener to prevent memory leaks
        if (userDocListener != null) {
            userDocListener.remove();
        }
    }
}