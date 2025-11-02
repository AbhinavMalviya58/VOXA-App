package com.WANGDULabs.VOXA;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.data.Models.SearchUser;
import com.WANGDULabs.VOXA.data.repository.SocialRepository;
import com.WANGDULabs.VOXA.ui.adapters.FollowRequestsAdapter;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class notifications extends AppCompatActivity {

    private RecyclerView requestsList;
    private FollowRequestsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestsList = findViewById(R.id.requestsList);
        if (requestsList != null) {
            requestsList.setLayoutManager(new LinearLayoutManager(this));
            adapter = new FollowRequestsAdapter(this, this::approve, this::deny);
            requestsList.setAdapter(adapter);
            loadIncomingRequests();
        }

        FooterController.bind(this, FooterController.Tab.NOTIF);
    }

    private void loadIncomingRequests() {
        String me = FirebaseAuth.getInstance().getUid();
        if (me == null) return;
        FirebaseFirestore.getInstance().collection("follow_requests").document(me).collection("incoming").limit(50)
                .get().addOnSuccessListener(snap -> {
                    List<SearchUser> items = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid = d.getId();
                        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(u -> {
                            if (u != null && u.exists()) {
                                String dn = u.getString("displayName");
                                String handle = u.getString("userId_lc");
                                String photo = u.getString("photoUrl");
                                items.add(new SearchUser(uid, dn, handle, photo));
                                adapter.setItems(items, false);
                            }
                        });
                    }
                });
    }

    private void approve(SearchUser u) {
        String me = FirebaseAuth.getInstance().getUid();
        if (me == null || u == null) return;
        new SocialRepository().approveRequest(me, u.getUid(), (success, state, e) -> loadIncomingRequests());
    }

    private void deny(SearchUser u) {
        String me = FirebaseAuth.getInstance().getUid();
        if (me == null || u == null) return;
        new SocialRepository().declineRequest(me, u.getUid(), (success, state, e) -> loadIncomingRequests());
    }
}