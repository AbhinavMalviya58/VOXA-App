package com.WANGDULabs.VOXA;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.WANGDULabs.VOXA.ui.activities.loginTab;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.net.Uri;
import android.provider.MediaStore;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import android.os.Handler;
import android.graphics.Color;

public class profilePage extends AppCompatActivity {
    TextView shoEmail,shoName,user,FamilyName,profileName,Gender;
    ImageView userImg;
    Button logout_button;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseDatabase database;
    FirebaseFirestore fs;
    FirebaseStorage storage;
    GoogleSignInClient gsc;
    GoogleSignInOptions gso;
    TextView xpValue, levelValue, totalWinsValue, currentStreakValue, highestStreakValue,
            stat_guess_number, stat_rps, stat_even_odd, stat_ttt, stat_math, stat_hl, rankValue;

    TextView bioValue, textUserId, followersCount, followingCount;
    TextInputLayout inputDisplayNameLayout, inputUserIdLayout, inputBioLayout;
    TextInputEditText inputDisplayName, inputUserId, inputBio;
    Button btnSaveProfile, btnCancelEdit, btnAddLink, btnEditLinks, btnFollow, btnShare;
    FloatingActionButton fabEdit;
    FloatingActionButton fabChangePhoto;
    LinearLayout socialLinksContainer;
    View savingProgress;

    String profileUid;
    boolean isSelf = true;
    boolean isEditing = false;
    boolean isFollowing = false;
    String currentDisplayName = "";
    String currentUserId = "";
    String currentBio = "";
    String currentPhotoUrl = "";
    Uri pickedImageUri = null;
    String uploadedPhotoUrl = null;
    Handler handler = new Handler();
    Runnable userIdCheckRunnable, nameCheckRunnable;
    boolean userIdOk = true, nameOk = true;
    ListenerRegistration userDocReg;
    int defaultHandleColor = Color.BLACK;

    private static final int REQ_PICK_IMAGE = 101;
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-z0-9_]{10}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
//        shoName = findViewById(R.id.shoName);
        shoEmail = findViewById(R.id.shoEmail);
        user = findViewById(R.id.user);
        logout_button = findViewById(R.id.logout_button);
        userImg = findViewById(R.id.userImg);
        Gender = findViewById(R.id.genderName);
        profileName = findViewById(R.id.profile);
        xpValue = findViewById(R.id.xpValue);
        levelValue = findViewById(R.id.levelValue);
        totalWinsValue = findViewById(R.id.totalWinsValue);
        currentStreakValue = findViewById(R.id.currentStreakValue);
        highestStreakValue = findViewById(R.id.highestStreakValue);
        stat_guess_number = findViewById(R.id.stat_guess_number);
        stat_rps = findViewById(R.id.stat_rps);
        stat_even_odd = findViewById(R.id.stat_even_odd);
        stat_ttt = findViewById(R.id.stat_ttt);
        stat_math = findViewById(R.id.stat_math);
        stat_hl = findViewById(R.id.stat_hl);
        rankValue = findViewById(R.id.rankValue);

        bioValue = findViewById(R.id.bioValue);
        textUserId = findViewById(R.id.textUserId);
        followersCount = findViewById(R.id.followersCount);
        followingCount = findViewById(R.id.followingCount);
        inputDisplayNameLayout = findViewById(R.id.inputDisplayNameLayout);
        inputUserIdLayout = findViewById(R.id.inputUserIdLayout);
        inputBioLayout = findViewById(R.id.inputBioLayout);
        inputDisplayName = findViewById(R.id.inputDisplayName);
        inputUserId = findViewById(R.id.inputUserId);
        inputBio = findViewById(R.id.inputBio);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnAddLink = findViewById(R.id.btnAddLink);
        btnEditLinks = findViewById(R.id.btnEditLinks);
        btnFollow = findViewById(R.id.btnFollow);
        btnShare = findViewById(R.id.btnShare);
        fabEdit = findViewById(R.id.fabEdit);
        fabChangePhoto = findViewById(R.id.fabChangePhoto);
        socialLinksContainer = findViewById(R.id.socialLinksContainer);
        savingProgress = findViewById(R.id.savingProgress);

        fs = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        String extraUid = getIntent().getStringExtra("uid");
        profileUid = (extraUid != null && !extraUid.isEmpty()) ? extraUid : (mUser != null ? mUser.getUid() : null);
        isSelf = mUser != null && profileUid != null && profileUid.equals(mUser.getUid());
        if (!isSelf) {
            fabEdit.setVisibility(View.GONE);
        } else {
            btnFollow.setVisibility(View.GONE);
        }


        if(mUser == null){
            Intent intent = new Intent(getApplicationContext(), loginTab.class);
            startActivity(intent);
            finish();
        }else {
            shoEmail.setText(mUser.getEmail());
        }
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null){
            String userName = account.getDisplayName();
            String userEmail = account.getEmail();
            String personFamilyName = account.getGivenName();
            user.setText(userName);
//            shoName.setText(userName);
            shoEmail.setText(userEmail);
            if (account.getPhotoUrl() != null) {
                Glide.with(this).load(account.getPhotoUrl()).circleCrop().into(userImg);
            }

//            profileName.setText(personFamilyName);
        }
        if (mUser != null && mUser.getPhotoUrl() != null) {
            Glide.with(this).load(mUser.getPhotoUrl()).circleCrop().into(userImg);
        }

        database = FirebaseDatabase.getInstance();
        if (mUser != null) {
            DatabaseReference ref = database.getReference("users").child(mUser.getUid());
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Long xp = snapshot.child("xp").getValue(Long.class);
                    Long level = snapshot.child("level").getValue(Long.class);
                    Long totalWins = snapshot.child("totalWins").getValue(Long.class);
                    Long currentStreak = snapshot.child("currentStreak").getValue(Long.class);
                    Long highestStreak = snapshot.child("highestStreak").getValue(Long.class);
                    if (xp != null) xpValue.setText(String.valueOf(xp));
                    if (level != null) levelValue.setText(String.valueOf(level));
                    if (totalWins != null) totalWinsValue.setText(String.valueOf(totalWins));
                    if (currentStreak != null) currentStreakValue.setText(String.valueOf(currentStreak));
                    if (highestStreak != null) highestStreakValue.setText(String.valueOf(highestStreak));

                    Long gtn = snapshot.child("gameStats").child("guess_the_number").getValue(Long.class);
                    Long rps = snapshot.child("gameStats").child("rock_paper_scissors").getValue(Long.class);
                    Long evenOdd = snapshot.child("gameStats").child("even_or_odd").getValue(Long.class);
                    Long ttt = snapshot.child("gameStats").child("tic_tac_toe").getValue(Long.class);
                    Long math = snapshot.child("mathQuizHighScore").getValue(Long.class);
                    Long hl = snapshot.child("gameStats").child("higher_lower").getValue(Long.class);

                    if (gtn == null) gtn = 0L;
                    if (rps == null) rps = 0L;
                    if (evenOdd == null) evenOdd = 0L;
                    if (ttt == null) ttt = 0L;
                    if (math == null) math = 0L;
                    if (hl == null) hl = 0L;

                    stat_guess_number.setText("Guess The Number: " + gtn);
                    stat_rps.setText("Rock Paper Scissors: " + rps);
                    stat_even_odd.setText("Even or Odd: " + evenOdd);
                    stat_ttt.setText("Tic Tac Toe: " + ttt);
                    stat_math.setText("Math Quiz High Score: " + math);
                    stat_hl.setText("Higher Lower: " + hl);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

            database.getReference("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long myXp = 0;
                    DataSnapshot me = snapshot.child(mUser.getUid());
                    Long val = me.child("xp").getValue(Long.class);
                    if (val != null) myXp = val;
                    int better = 0;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Long xp = child.child("xp").getValue(Long.class);
                        if (xp != null && xp > myXp) better++;
                    }
                    int rank = better + 1;
                    rankValue.setText(String.valueOf(rank));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }

        if (profileUid != null) {
            attachUserDocListener();
            if (!isSelf) observeFollowingState();
        }

        if (followersCount != null) followersCount.setOnClickListener(v -> openFollowers());
        if (followingCount != null) followingCount.setOnClickListener(v -> openFollowing());
        if (btnShare != null) btnShare.setOnClickListener(v -> shareProfile());
        if (btnFollow != null) btnFollow.setOnClickListener(v -> toggleFollow());
        if (fabEdit != null) fabEdit.setOnClickListener(v -> setEditMode(!isEditing));
        if (btnCancelEdit != null) btnCancelEdit.setOnClickListener(v -> setEditMode(false));
        if (btnSaveProfile != null) btnSaveProfile.setOnClickListener(v -> saveProfile());
        if (btnAddLink != null) btnAddLink.setOnClickListener(v -> addSocialLinkDialog());
        if (btnEditLinks != null) btnEditLinks.setOnClickListener(v -> addSocialLinkDialog());
        if (userImg != null) userImg.setOnClickListener(v -> { if (isEditing) pickImage(); });
        if (fabChangePhoto != null) fabChangePhoto.setOnClickListener(v -> { if (isEditing) pickImage(); });

        // Enable counters on inputs for better UX
        if (inputDisplayNameLayout != null) { inputDisplayNameLayout.setCounterEnabled(true); inputDisplayNameLayout.setCounterMaxLength(9); }
        if (inputUserIdLayout != null) { inputUserIdLayout.setCounterEnabled(true); inputUserIdLayout.setCounterMaxLength(10); }
        if (textUserId != null) defaultHandleColor = textUserId.getCurrentTextColor();

        if (inputUserId != null) inputUserId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (userIdCheckRunnable != null) handler.removeCallbacks(userIdCheckRunnable);
                userIdCheckRunnable = () -> checkUserIdAvailability(s.toString().trim());
                handler.postDelayed(userIdCheckRunnable, 1500);
                if (textUserId != null) {
                    textUserId.setText("@" + s.toString().trim());
                    boolean ok = USER_ID_PATTERN.matcher(s.toString().trim()).matches();
                    textUserId.setTextColor(ok ? defaultHandleColor : Color.RED);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        if (inputDisplayName != null) inputDisplayName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (nameCheckRunnable != null) handler.removeCallbacks(nameCheckRunnable);
                nameCheckRunnable = () -> checkDisplayNameAvailability(s.toString().trim());
                handler.postDelayed(nameCheckRunnable, 1500);
                if (user != null) user.setText(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), loginTab.class);
                startActivity(intent);
                finish();
//                logout();
            }
        });

//        Toolbar
        Toolbar toolbar = findViewById(R.id.materialToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    private void attachUserDocListener() {
        DocumentReference ref = FirebaseFirestore.getInstance().collection("users").document(profileUid);
        userDocReg = ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                if (snapshot == null) return;
                String dn = snapshot.getString("displayName");
                String uidLc = snapshot.getString("userId_lc");
                String bio = snapshot.getString("bio");
                Long followers = snapshot.getLong("followersCount");
                Long following = snapshot.getLong("followingCount");
                currentPhotoUrl = snapshot.getString("photoUrl");
                currentDisplayName = dn != null ? dn : "";
                currentUserId = uidLc != null ? uidLc : "";
                currentBio = bio != null ? bio : "";
                if (!isEditing) {
                    user.setText(currentDisplayName.isEmpty()? user.getText(): currentDisplayName);
                    textUserId.setText(currentUserId.isEmpty()? textUserId.getText(): ("@"+currentUserId));
                    bioValue.setText(currentBio);
                    if (followers != null) followersCount.setText(followers + " Followers");
                    if (following != null) followingCount.setText(following + " Following");
                    if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
                        Glide.with(profilePage.this).load(currentPhotoUrl).circleCrop().into(userImg);
                    }
                }
                List<Map<String,Object>> links = (List<Map<String,Object>>) snapshot.get("socialLinks");
                renderSocialLinks(links);
            }
        });
    }

    private void renderSocialLinks(List<Map<String,Object>> links) {
        socialLinksContainer.removeAllViews();
        if (links == null) return;
        for (Map<String,Object> link : links) {
            String name = link.get("name") != null ? String.valueOf(link.get("name")) : "";
            String url = link.get("url") != null ? String.valueOf(link.get("url")) : "";
            TextView tv = new TextView(this);
            tv.setText(name + " - " + url);
            tv.setTextSize(14f);
            tv.setPadding(8,8,8,8);
            tv.setOnClickListener(v -> {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                    startActivity(Intent.createChooser(i, "Open link"));
                } catch (Exception ignored) {}
            });
            socialLinksContainer.addView(tv);
        }
    }

    private void setEditMode(boolean edit) {
        if (!isSelf) return;
        isEditing = edit;
        animateVisibility(inputDisplayNameLayout, edit);
        animateVisibility(inputUserIdLayout, edit);
        animateVisibility(inputBioLayout, edit);
        animateVisibility(btnSaveProfile, edit);
        animateVisibility(btnCancelEdit, edit);
        animateVisibility(fabChangePhoto, edit);
        if (edit) {
            inputDisplayName.setText(currentDisplayName);
            inputUserId.setText(currentUserId);
            inputBio.setText(currentBio);
        }
    }

    private void pickImage() {
        Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pick, REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pickedImageUri = data.getData();
            userImg.setImageURI(pickedImageUri);
        }
    }

    private void uploadPhotoIfNeeded(Runnable then) {
        if (pickedImageUri == null) { then.run(); return; }
        if (mUser == null) { then.run(); return; }
        StorageReference ref = storage.getReference().child("profile_photos").child(mUser.getUid()+".jpg");
        ref.putFile(pickedImageUri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            uploadedPhotoUrl = uri.toString();
            then.run();
        }).addOnFailureListener(e -> then.run())).addOnFailureListener(e -> then.run());
    }

    private void saveProfile() {
        if (inputDisplayName == null || inputUserId == null) return;
        final String newName = inputDisplayName.getText() != null ? inputDisplayName.getText().toString().trim() : "";
        final String newId = inputUserId.getText() != null ? inputUserId.getText().toString().trim().toLowerCase() : "";
        final String newBio = inputBio != null && inputBio.getText()!=null ? inputBio.getText().toString().trim() : "";
        if (newName.length() < 3 || newName.length() > 9) {
            inputDisplayNameLayout.setError("Display name must be 3–9 chars");
            return;
        } else inputDisplayNameLayout.setError(null);
        if (!USER_ID_PATTERN.matcher(newId).matches()) {
            inputUserIdLayout.setError("User ID must be 10 chars: a-z 0-9 _");
            return;
        } else inputUserIdLayout.setError(null);
        if (!userIdOk || !nameOk) return;
        btnSaveProfile.setEnabled(false);
        btnCancelEdit.setEnabled(false);
        if (fabEdit != null) fabEdit.setEnabled(false);
        if (fabChangePhoto != null) fabChangePhoto.setEnabled(false);
        if (savingProgress != null) savingProgress.setVisibility(View.VISIBLE);
        uploadPhotoIfNeeded(() -> runUserSaveTransaction(newName, newId, newBio));
    }

    private void runUserSaveTransaction(final String newName, final String newId, final String newBio) {
        if (mUser == null) return;
        FirebaseFirestore.getInstance().runTransaction(transaction -> {
            DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(mUser.getUid());
            DocumentSnapshot snap = transaction.get(userRef);
            String oldId = snap.getString("userId_lc");
            String oldNameLc = snap.getString("displayName_lc");
            DocumentReference idRef = FirebaseFirestore.getInstance().collection("userIds").document(newId);
            DocumentReference nameRef = FirebaseFirestore.getInstance().collection("displayNames").document(newName.toLowerCase());
            if (oldId == null || !oldId.equals(newId)) {
                DocumentSnapshot idSnap = transaction.get(idRef);
                if (idSnap.exists() && !Objects.equals(idSnap.getString("uid"), mUser.getUid())) {
                    throw new RuntimeException("User ID already taken");
                }
            }
            if (oldNameLc == null || !oldNameLc.equals(newName.toLowerCase())) {
                DocumentSnapshot nameSnap = transaction.get(nameRef);
                if (nameSnap.exists() && !Objects.equals(nameSnap.getString("uid"), mUser.getUid())) {
                    throw new RuntimeException("Display name already taken");
                }
            }
            Map<String,Object> up = new HashMap<>();
            up.put("displayName", newName);
            up.put("displayName_lc", newName.toLowerCase());
            up.put("userId_lc", newId);
            up.put("bio", newBio);
            if (uploadedPhotoUrl != null) up.put("photoUrl", uploadedPhotoUrl);
            transaction.set(userRef, up, SetOptions.merge());
            if (oldId == null || !oldId.equals(newId)) {
                if (oldId != null && !oldId.isEmpty()) transaction.delete(FirebaseFirestore.getInstance().collection("userIds").document(oldId));
                Map<String,Object> idMap = new HashMap<>(); idMap.put("uid", mUser.getUid());
                transaction.set(idRef, idMap, SetOptions.merge());
            }
            if (oldNameLc == null || !oldNameLc.equals(newName.toLowerCase())) {
                if (oldNameLc != null && !oldNameLc.isEmpty()) transaction.delete(FirebaseFirestore.getInstance().collection("displayNames").document(oldNameLc));
                Map<String,Object> nm = new HashMap<>(); nm.put("uid", mUser.getUid());
                transaction.set(nameRef, nm, SetOptions.merge());
            }
            return null;
        }).addOnSuccessListener(r -> {
            btnSaveProfile.setEnabled(true);
            btnCancelEdit.setEnabled(true);
            if (fabEdit != null) fabEdit.setEnabled(true);
            if (fabChangePhoto != null) fabChangePhoto.setEnabled(true);
            if (savingProgress != null) savingProgress.setVisibility(View.GONE);
            isEditing = false;
            setEditMode(false);
            Toast.makeText(profilePage.this, "Profile saved", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            btnSaveProfile.setEnabled(true);
            btnCancelEdit.setEnabled(true);
            if (fabEdit != null) fabEdit.setEnabled(true);
            if (fabChangePhoto != null) fabChangePhoto.setEnabled(true);
            if (savingProgress != null) savingProgress.setVisibility(View.GONE);
            Toast.makeText(profilePage.this, String.valueOf(e.getMessage()), Toast.LENGTH_SHORT).show();
        });
    }

    private void animateVisibility(View v, boolean show) {
        if (v == null) return;
        if (show) {
            if (v.getVisibility() != View.VISIBLE) {
                v.setAlpha(0f);
                v.setVisibility(View.VISIBLE);
                v.animate().alpha(1f).setDuration(150).start();
            }
        } else {
            if (v.getVisibility() == View.VISIBLE) {
                v.animate().alpha(0f).setDuration(120).withEndAction(() -> v.setVisibility(View.GONE)).start();
            }
        }
    }

    private void checkUserIdAvailability(String candidate) {
        if (TextUtils.isEmpty(candidate)) return;
        if (!USER_ID_PATTERN.matcher(candidate).matches()) { inputUserIdLayout.setError("Invalid format"); userIdOk = false; return; }
        FirebaseFirestore.getInstance().collection("userIds").document(candidate).get().addOnSuccessListener(doc -> {
            boolean taken = doc.exists() && !Objects.equals(doc.getString("uid"), mUser != null ? mUser.getUid() : null);
            userIdOk = !taken;
            inputUserIdLayout.setError(taken ? "User ID already taken" : null);
        });
    }

    private void checkDisplayNameAvailability(String candidate) {
        if (TextUtils.isEmpty(candidate)) return;
        FirebaseFirestore.getInstance().collection("displayNames").document(candidate.toLowerCase()).get().addOnSuccessListener(doc -> {
            boolean taken = doc.exists() && !Objects.equals(doc.getString("uid"), mUser != null ? mUser.getUid() : null);
            nameOk = !taken;
            inputDisplayNameLayout.setError(taken ? "Display name already taken" : null);
        });
    }

    private void addSocialLinkDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText name = new EditText(this); name.setHint("Label");
        EditText url = new EditText(this); url.setHint("https://...");
        layout.addView(name); layout.addView(url);
        new AlertDialog.Builder(this).setTitle("Add Social Link").setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String n = name.getText().toString().trim();
                    String u = url.getText().toString().trim();
                    if (n.isEmpty() || u.isEmpty()) return;
                    Map<String,Object> link = new HashMap<>(); link.put("name", n); link.put("url", u);
                    DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(profileUid);
                    userRef.update("socialLinks", FieldValue.arrayUnion(link));
                }).setNegativeButton("Cancel", null).show();
    }

    private void openFollowers() {
        Intent i = new Intent(this, com.WANGDULabs.VOXA.ui.activities.FollowersListActivity.class);
        i.putExtra("uid", profileUid);
        startActivity(i);
    }
    private void openFollowing() {
        Intent i = new Intent(this, com.WANGDULabs.VOXA.ui.activities.FollowingListActivity.class);
        i.putExtra("uid", profileUid);
        startActivity(i);
    }

    private void toggleFollow() {
        if (mUser == null || profileUid == null || isSelf) return;
        final String me = mUser.getUid();
        final String other = profileUid;
        if (isFollowing) {
            FirebaseFirestore.getInstance().runBatch(batch -> {
                batch.delete(FirebaseFirestore.getInstance().collection("follows").document(me).collection("following").document(other));
                batch.delete(FirebaseFirestore.getInstance().collection("follows").document(other).collection("followers").document(me));
            }).addOnSuccessListener(v -> Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show());
        } else {
            FirebaseFirestore.getInstance().runBatch(batch -> {
                Map<String,Object> meta = new HashMap<>(); meta.put("createdAt", FieldValue.serverTimestamp());
                batch.set(FirebaseFirestore.getInstance().collection("follows").document(me).collection("following").document(other), meta);
                batch.set(FirebaseFirestore.getInstance().collection("follows").document(other).collection("followers").document(me), meta);
            }).addOnSuccessListener(v -> Toast.makeText(this, "Following", Toast.LENGTH_SHORT).show());
        }
    }

    private void observeFollowingState() {
        if (mUser == null) return;
        FirebaseFirestore.getInstance().collection("follows").document(mUser.getUid()).collection("following").document(profileUid)
                .addSnapshotListener((s, e) -> {
                    isFollowing = s != null && s.exists();
                    btnFollow.setText(isFollowing ? "Unfollow" : "Follow");
                });
    }

    private void shareProfile() {
        String id = currentUserId != null && !currentUserId.isEmpty() ? currentUserId : (mUser != null ? mUser.getUid() : "");
        String text = "Check out @" + id + " on VOXA: https://voxa.app/user/" + id;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, "Share profile"));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    void logout(){
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                finish();
                startActivity(new Intent(profilePage.this, loginTab.class));
            }
        });
    }
}