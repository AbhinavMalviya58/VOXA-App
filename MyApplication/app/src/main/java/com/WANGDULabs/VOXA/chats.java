package com.WANGDULabs.VOXA;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.data.Models.ChatMessage;
import com.WANGDULabs.VOXA.ui.adapters.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class chats extends AppCompatActivity {
    private RecyclerView messagesList;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter adapter;
    private DatabaseReference messagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        messagesList = findViewById(R.id.messagesList);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        adapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        messagesList.setLayoutManager(lm);
        messagesList.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String roomId = "public";
        messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(roomId).child("messages");
        messagesRef.keepSynced(true);

        messagesRef.limitToLast(200).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                ChatMessage m = snapshot.getValue(ChatMessage.class);
                if (m != null) {
                    adapter.addMessage(m);
                    messagesList.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
            @Override public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(DataSnapshot snapshot) {}
            @Override public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(DatabaseError error) {}
        });

        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            String uid = user != null ? user.getUid() : "anonymous";
            long now = System.currentTimeMillis();
            DatabaseReference newMsg = messagesRef.push();
            ChatMessage msg = new ChatMessage(newMsg.getKey(), uid, text, now);
            newMsg.setValue(msg);
            messageInput.setText("");
        });
    }
}