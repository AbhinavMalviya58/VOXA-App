package com.WANGDULabs.VOXA.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.RecyclerUserModelAdapter;
import com.WANGDULabs.VOXA.userModel;
import com.WANGDULabs.VOXA.profilePage;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;
import com.WANGDULabs.VOXA.ui.adapters.SearchResultsAdapter;
import com.WANGDULabs.VOXA.data.Models.SearchUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private final ArrayList<userModel> arrUserModel = new ArrayList<>();

    private FirebaseAuth mAuth;
    private RecyclerView searchResults;
    private SearchResultsAdapter searchAdapter;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private FirebaseFirestore fs;
    private DocumentSnapshot lastNameDoc, lastUserIdDoc;
    private boolean isLoading = false;
    private boolean hasMoreName = true, hasMoreUserId = true;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        if (mUser == null) {
            startActivity(new Intent(MainActivity.this, loginTab.class));
            finish();
            return;
        }

        ImageView profilePic = findViewById(R.id.profilePic);
        ImageView chatIcon = findViewById(R.id.chatIcon);
        if (profilePic != null) {
            if (mUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(mUser.getPhotoUrl())
                        .placeholder(R.drawable.bglogo)
                        .circleCrop()
                        .into(profilePic);
            }
            profilePic.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, profilePage.class)));
        }
        if (chatIcon != null) {
            chatIcon.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ChatListActivity.class)));
        }

        recyclerView = findViewById(R.id.media);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        arrUserModel.add(new userModel(R.drawable.logo, R.drawable.facebook, "Abhinav Malviya", "hai ", "@string/random_text"));
        arrUserModel.add(new userModel(R.drawable.logo, R.drawable.dp, "Abhinav Malviya", "hai ", "@string/random_text"));
        arrUserModel.add(new userModel(R.drawable.logo, R.drawable.google, "Abhinav Malviya", "hai ", "@string/random_text"));

        RecyclerUserModelAdapter adapter = new RecyclerUserModelAdapter(this, arrUserModel, position -> {
            Toast.makeText(MainActivity.this, "Item " + position, Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

        fs = FirebaseFirestore.getInstance();
        searchResults = findViewById(R.id.searchResults);
        searchResults.setLayoutManager(new LinearLayoutManager(this));
        String myUid = mUser.getUid();
        searchAdapter = new SearchResultsAdapter(this, u -> {
            Intent i = new Intent(MainActivity.this, profilePage.class);
            i.putExtra("uid", u.getUid());
            startActivity(i);
        }, u -> toggleFollow(myUid, u.getUid()), myUid);
        searchResults.setAdapter(searchAdapter);

        EditText searchInput = findViewById(R.id.Search);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> {
                        String q = s.toString().trim().toLowerCase();
                        currentQuery = q;
                        if (q.isEmpty()) {
                            searchResults.setVisibility(View.GONE);
                            View feed = findViewById(R.id.feedContainer);
                            if (feed != null) feed.setVisibility(View.VISIBLE);
                            return;
                        }
                        View feed = findViewById(R.id.feedContainer);
                        if (feed != null) feed.setVisibility(View.GONE);
                        searchResults.setVisibility(View.VISIBLE);
                        lastNameDoc = null; lastUserIdDoc = null; hasMoreName = true; hasMoreUserId = true;
                        runSearch(q, false);
                    };
                    searchHandler.postDelayed(searchRunnable, 1500);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        searchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int total = searchAdapter.getItemCount();
                int last = lm.findLastVisibleItemPosition();
                if (!isLoading && (hasMoreName || hasMoreUserId) && last >= total - 4) {
                    runSearch(currentQuery, true);
                }
            }
        });

        FooterController.bind(this, FooterController.Tab.HOME);
    }

    private void runSearch(String q, boolean append) {
        if (isLoading) return;
        isLoading = true;
        Query qNameQ = fs.collection("users").orderBy("displayName_lc").startAt(q).endAt(q + "\uf8ff").limit(20);
        Query qUserQ = fs.collection("users").orderBy("userId_lc").startAt(q).endAt(q + "\uf8ff").limit(20);
        if (append) {
            if (lastNameDoc != null) qNameQ = qNameQ.startAfter(lastNameDoc);
            if (lastUserIdDoc != null) qUserQ = qUserQ.startAfter(lastUserIdDoc);
        }
        final Query qNameFinal = qNameQ;
        final Query qUserFinal = qUserQ;
        qNameFinal.get().addOnSuccessListener(snap1 -> {
            qUserFinal.get().addOnSuccessListener(snap2 -> {
                List<SearchUser> res = new ArrayList<>();
                Set<String> seen = new HashSet<>();
                if (snap1 != null && !snap1.isEmpty()) {
                    List<DocumentSnapshot> docs = snap1.getDocuments();
                    lastNameDoc = docs.get(docs.size() - 1);
                    for (DocumentSnapshot d : docs) {
                        String uid = d.getId();
                        if (seen.add(uid)) {
                            String dn = d.getString("displayName");
                            String handle = d.getString("userId_lc");
                            String photo = d.getString("photoUrl");
                            res.add(new SearchUser(uid, dn, handle, photo));
                        }
                    }
                } else {
                    hasMoreName = false;
                }
                if (snap2 != null && !snap2.isEmpty()) {
                    List<DocumentSnapshot> docs2 = snap2.getDocuments();
                    lastUserIdDoc = docs2.get(docs2.size() - 1);
                    for (DocumentSnapshot d : docs2) {
                        String uid = d.getId();
                        if (seen.add(uid)) {
                            String dn = d.getString("displayName");
                            String handle = d.getString("userId_lc");
                            String photo = d.getString("photoUrl");
                            res.add(new SearchUser(uid, dn, handle, photo));
                        }
                    }
                } else {
                    hasMoreUserId = false;
                }
                searchAdapter.setItems(res, append);
                isLoading = false;
            }).addOnFailureListener(e -> { isLoading = false; });
        }).addOnFailureListener(e -> { isLoading = false; });
    }

    private void toggleFollow(String me, String other) {
        if (me == null || other == null || me.equals(other)) return;
        FirebaseFirestore.getInstance().collection("follows").document(me).collection("following").document(other).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        FirebaseFirestore.getInstance().runBatch(batch -> {
                            batch.delete(FirebaseFirestore.getInstance().collection("follows").document(me).collection("following").document(other));
                            batch.delete(FirebaseFirestore.getInstance().collection("follows").document(other).collection("followers").document(me));
                        });
                    } else {
                        FirebaseFirestore.getInstance().runBatch(batch -> {
                            Map<String,Object> meta = new HashMap<>(); meta.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                            batch.set(FirebaseFirestore.getInstance().collection("follows").document(me).collection("following").document(other), meta);
                            batch.set(FirebaseFirestore.getInstance().collection("follows").document(other).collection("followers").document(me), meta);
                        });
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}