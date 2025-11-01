package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.ChatMessage;
import com.WANGDULabs.VOXA.ui.adapters.ChatMessagesAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatDetailFirestoreActivity extends AppCompatActivity {

    private RecyclerView messagesList;
    private EditText messageInput;
    private ImageButton sendBtn;
    private ChatMessagesAdapter adapter;

    private String myUid;
    private String otherUid;
    private String conversationId;

    private final FirebaseFirestore fs = FirebaseFirestore.getInstance();
    private ListenerRegistration messagesReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail_firestore);

        myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) {
            finish();
            return;
        }

        otherUid = getIntent().getStringExtra("otherUid");
        conversationId = getIntent().getStringExtra("conversationId");
        if (TextUtils.isEmpty(conversationId) && !TextUtils.isEmpty(otherUid)) {
            conversationId = deterministicConversationId(myUid, otherUid);
        }
        if (TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, "Missing conversation", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messagesList = findViewById(R.id.messagesList);
        messageInput = findViewById(R.id.messageInput);
        sendBtn = findViewById(R.id.sendBtn);

        messagesList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessagesAdapter(myUid);
        messagesList.setAdapter(adapter);

        attachMessagesListener();

        sendBtn.setOnClickListener(v -> attemptSend());
    }

    private void attachMessagesListener() {
        Query q = fs.collection("conversations").document(conversationId)
                .collection("messages").orderBy("createdAt", Query.Direction.ASCENDING).limit(500);
        messagesReg = q.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null || snapshots == null) return;
                List<ChatMessage> list = new ArrayList<>();
                for (DocumentSnapshot d : snapshots.getDocuments()) {
                    ChatMessage m = new ChatMessage();
                    m.setId(d.getId());
                    m.setSenderId(d.getString("senderId"));
                    m.setText(d.getString("text"));
                    Long ts = null;
                    try { ts = d.getTimestamp("createdAt").toDate().getTime(); } catch (Exception ignored) {}
                    m.setTimestamp(ts == null ? 0 : ts);
                    list.add(m);
                }
                adapter.setItems(list);
                messagesList.scrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    private void attemptSend() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;
        if (TextUtils.isEmpty(otherUid)) {
            // If otherUid not provided, cannot enforce follow gating
            actuallySend(text);
            return;
        }
        fs.collection("follows").document(myUid).collection("following").document(otherUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        actuallySend(text);
                    } else {
                        Toast.makeText(this, "Follow the user to send messages", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(err -> actuallySend(text)); // fail-open to avoid blocking in dev
    }

    private void actuallySend(String text) {
        ensureConversation();
        DocumentReference convRef = fs.collection("conversations").document(conversationId);
        DocumentReference msgRef = convRef.collection("messages").document();
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", myUid);
        data.put("text", text);
        data.put("createdAt", FieldValue.serverTimestamp());
        msgRef.set(data).addOnSuccessListener(v -> {
            Map<String, Object> up = new HashMap<>();
            up.put("lastMessage", text);
            up.put("updatedAt", FieldValue.serverTimestamp());
            List<String> parts = new ArrayList<>();
            parts.add(myUid);
            if (!TextUtils.isEmpty(otherUid)) parts.add(otherUid);
            up.put("participants", parts);
            convRef.set(up, com.google.firebase.firestore.SetOptions.merge());
            messageInput.setText("");
        });
    }

    private void ensureConversation() {
        DocumentReference convRef = fs.collection("conversations").document(conversationId);
        convRef.get().addOnSuccessListener(doc -> {
            if (doc != null && !doc.exists()) {
                Map<String, Object> init = new HashMap<>();
                List<String> parts = new ArrayList<>();
                parts.add(myUid);
                if (!TextUtils.isEmpty(otherUid)) parts.add(otherUid);
                init.put("participants", parts);
                init.put("updatedAt", FieldValue.serverTimestamp());
                convRef.set(init);
            }
        });
    }

    private String deterministicConversationId(String a, String b) {
        return (a.compareTo(b) < 0) ? (a + "_" + b) : (b + "_" + a);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesReg != null) messagesReg.remove();
    }
}
