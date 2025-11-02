package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatDetailFirestoreActivity extends AppCompatActivity {

    private RecyclerView messagesList;
    private EditText messageInput;
    private ImageButton sendBtn;
    private ImageButton attachBtn;
    private ChatMessagesAdapter adapter;

    private String myUid;
    private String otherUid;
    private String conversationId;

    private final FirebaseFirestore fs = FirebaseFirestore.getInstance();
    private ListenerRegistration messagesReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_detail_firestore);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
        attachBtn = findViewById(R.id.attachBtn);

        messagesList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessagesAdapter(myUid, otherUid);
        messagesList.setAdapter(adapter);
        adapter.setOnLongClick(m -> showReactionDialog(m));

        attachMessagesListener();

        sendBtn.setOnClickListener(v -> attemptSend());
        if (attachBtn != null) attachBtn.setOnClickListener(v -> pickImage());

        com.WANGDULabs.VOXA.ui.navigation.FooterController.bind(this, com.WANGDULabs.VOXA.ui.navigation.FooterController.Tab.HOME);
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
                    m.setImageUrl(d.getString("imageUrl"));
                    Long ts = null;
                    try { ts = d.getTimestamp("createdAt").toDate().getTime(); } catch (Exception ignored) {}
                    m.setTimestamp(ts == null ? 0 : ts);
                    try { List<String> delivered = (List<String>) d.get("deliveredTo"); if (delivered != null) m.setDeliveredTo(delivered); } catch (Exception ignored) {}
                    try { List<String> seen = (List<String>) d.get("seenBy"); if (seen != null) m.setSeenBy(seen); } catch (Exception ignored) {}
                    try { java.util.Map<String,String> rx = (java.util.Map<String,String>) d.get("reactions"); if (rx != null) m.setReactions(rx); } catch (Exception ignored) {}
                    list.add(m);
                }
                adapter.setItems(list);
                messagesList.scrollToPosition(adapter.getItemCount() - 1);

                // mark delivered/seen
                for (DocumentSnapshot d : snapshots.getDocuments()) {
                    String sid = d.getString("senderId");
                    if (sid == null) continue;
                    DocumentReference mr = d.getReference();
                    if (!sid.equals(myUid)) {
                        mr.update("deliveredTo", FieldValue.arrayUnion(myUid));
                        mr.update("seenBy", FieldValue.arrayUnion(myUid));
                    } else {
                        mr.update("deliveredTo", FieldValue.arrayUnion(myUid));
                    }
                }
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
        // Allow if other user is public OR mutual follow
        fs.collection("users").document(otherUid).get().addOnSuccessListener(userDoc -> {
            boolean isPrivate = false;
            try { Boolean b = userDoc.getBoolean("isPrivate"); isPrivate = b != null && b; } catch (Exception ignored) {}
            if (!isPrivate) { actuallySend(text); return; }
            fs.collection("follows").document(myUid).collection("following").document(otherUid).get()
                    .addOnSuccessListener(f1 -> {
                        if (f1 != null && f1.exists()) {
                            fs.collection("follows").document(otherUid).collection("following").document(myUid).get()
                                    .addOnSuccessListener(f2 -> {
                                        if (f2 != null && f2.exists()) {
                                            actuallySend(text);
                                        } else {
                                            Toast.makeText(this, "Messaging allowed after mutual follow", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Follow the user to send messages", Toast.LENGTH_SHORT).show();
                        }
                    });
        }).addOnFailureListener(err -> actuallySend(text));
    }

    private void actuallySend(String text) {
        ensureConversation();
        DocumentReference convRef = fs.collection("conversations").document(conversationId);
        DocumentReference msgRef = convRef.collection("messages").document();
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", myUid);
        data.put("text", text);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("deliveredTo", new ArrayList<String>());
        data.put("seenBy", new ArrayList<String>());
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

    private static final int REQ_PICK_IMAGE = 9911;
    private void pickImage() {
        try {
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(android.content.Intent.createChooser(i, "Select Image"), REQ_PICK_IMAGE);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri uri = data.getData();
            sendImage(uri);
        }
    }

    private void sendImage(android.net.Uri uri) {
        if (uri == null) return;
        ensureConversation();
        DocumentReference convRef = fs.collection("conversations").document(conversationId);
        DocumentReference msgRef = convRef.collection("messages").document();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("chat_images").child(conversationId).child(msgRef.getId()+".jpg");
        ref.putFile(uri).addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(download -> {
            Map<String, Object> data = new HashMap<>();
            data.put("senderId", myUid);
            data.put("imageUrl", download.toString());
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("deliveredTo", new ArrayList<String>());
            data.put("seenBy", new ArrayList<String>());
            msgRef.set(data).addOnSuccessListener(v -> {
                Map<String, Object> up = new HashMap<>();
                up.put("lastMessage", "📷 Photo");
                up.put("updatedAt", FieldValue.serverTimestamp());
                List<String> parts = new ArrayList<>();
                parts.add(myUid);
                if (!TextUtils.isEmpty(otherUid)) parts.add(otherUid);
                up.put("participants", parts);
                convRef.set(up, com.google.firebase.firestore.SetOptions.merge());
            });
        }));
    }

    private void showReactionDialog(ChatMessage m) {
        if (m == null || m.getId() == null) return;
        final String[] emojis = new String[]{"👍","❤️","😂","🔥","😮","🙏"};
        new AlertDialog.Builder(this)
                .setTitle("React")
                .setItems(emojis, (d, which) -> {
                    String emoji = emojis[which];
                    DocumentReference mr = fs.collection("conversations").document(conversationId)
                            .collection("messages").document(m.getId());
                    Map<String, Object> up = new HashMap<>();
                    up.put("reactions." + myUid, emoji);
                    mr.update(up);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesReg != null) messagesReg.remove();
    }
}
