package com.WANGDULabs.VOXA.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.SearchUser;
import com.WANGDULabs.VOXA.data.repository.SocialRepository;
import com.WANGDULabs.VOXA.ui.adapters.FollowUserAdapter;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FollowersListActivity extends AppCompatActivity {
    private RecyclerView list;
    private FollowUserAdapter adapter;
    private String uid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_follow_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        uid = getIntent().getStringExtra("uid");
        list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FollowUserAdapter(this, this::openProfile, this::toggleFollow);
        list.setAdapter(adapter);
        loadFollowers();
        FooterController.bind(this, FooterController.Tab.HOME);
    }

    private void loadFollowers() {
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("follows").document(uid).collection("followers").limit(50)
                .get().addOnSuccessListener(snap -> {
                    List<SearchUser> items = new ArrayList<>();
                    List<DocumentSnapshot> docs = snap.getDocuments();
                    for (DocumentSnapshot d : docs) {
                        String other = d.getId();
                        FirebaseFirestore.getInstance().collection("users").document(other).get()
                                .addOnSuccessListener(u -> {
                                    if (u != null && u.exists()) {
                                        String dn = u.getString("displayName");
                                        String handle = u.getString("userId_lc");
                                        String photo = u.getString("photoUrl");
                                        items.add(new SearchUser(other, dn, handle, photo));
                                        adapter.setItems(items, false);
                                    }
                                });
                    }
                });
    }

    private void openProfile(SearchUser u) {
        Intent i = new Intent(this, com.WANGDULabs.VOXA.profilePage.class);
        i.putExtra("uid", u.getUid());
        startActivity(i);
    }

    private void toggleFollow(SearchUser u) {
        String me = FirebaseAuth.getInstance().getUid();
        if (me == null || u == null || u.getUid() == null || me.equals(u.getUid())) return;
        new SocialRepository().toggleFollowOrRequest(me, u.getUid(), (success, state, e) -> {});
    }
}
