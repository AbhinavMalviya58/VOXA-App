package com.WANGDULabs.VOXA.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.ChatListItem;
import com.WANGDULabs.VOXA.ui.adapters.ChatListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity {
    private RecyclerView chatList;
    private ChatListAdapter adapter;
    private ListenerRegistration reg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        chatList = findViewById(R.id.chatList);
        chatList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(item -> {
            Intent i = new Intent(ChatListActivity.this, ChatDetailFirestoreActivity.class);
            i.putExtra("conversationId", item.getConversationId());
            i.putExtra("otherUid", item.getOtherUid());
            startActivity(i);
        });
        chatList.setAdapter(adapter);

        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        Query q = FirebaseFirestore.getInstance()
                .collection("conversations")
                .whereArrayContains("participants", myUid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50);

        reg = q.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null || snapshots == null) return;
                List<ChatListItem> items = new ArrayList<>();
                Map<String, DocumentSnapshot> userCache = new HashMap<>();
                List<DocumentSnapshot> docs = snapshots.getDocuments();
                for (DocumentSnapshot d : docs) {
                    List<String> parts = (List<String>) d.get("participants");
                    if (parts == null || parts.size() < 2) continue;
                    String other = parts.get(0).equals(myUid) ? parts.get(1) : parts.get(0);
                    String last = d.getString("lastMessage");
                    Long updated = null;
                    try { updated = d.getTimestamp("updatedAt").toDate().getTime(); } catch (Exception ignored) {}
                    String convId = d.getId();
                    ChatListItem base = new ChatListItem(convId, other, "", "", last == null ? "" : last, updated == null ? 0 : updated);
                    items.add(base);
                }
                // Fetch user profiles for display (simple sequential fetch to keep code short)
                for (int i = 0; i < items.size(); i++) {
                    ChatListItem item = items.get(i);
                    FirebaseFirestore.getInstance().collection("users").document(item.getOtherUid()).get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc != null && userDoc.exists()) {
                                    item.setDisplayName(userDoc.getString("displayName"));
                                    item.setPhotoUrl(userDoc.getString("photoUrl"));
                                    adapter.notifyDataSetChanged();
                                }
                            });
                }
                adapter.setItems(items, false);
            }
        });

        com.WANGDULabs.VOXA.ui.navigation.FooterController.bind(this, com.WANGDULabs.VOXA.ui.navigation.FooterController.Tab.HOME);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reg != null) reg.remove();
    }
}
