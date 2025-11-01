package com.WANGDULabs.VOXA.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String currentUid = FirebaseAuth.getInstance().getUid();

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage m) {
        messages.add(m);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = messages.get(position);
        if (m.getSenderId() != null && m.getSenderId().equals(currentUid)) return TYPE_SENT;
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage m = messages.get(position);
        if (holder instanceof SentVH) {
            ((SentVH) holder).bind(m);
        } else if (holder instanceof ReceivedVH) {
            ((ReceivedVH) holder).bind(m);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentVH extends RecyclerView.ViewHolder {
        TextView body, time;
        SentVH(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.messageBody);
            time = itemView.findViewById(R.id.messageTime);
        }
        void bind(ChatMessage m) {
            body.setText(m.getText());
            time.setText(format(m.getTimestamp()));
        }
    }

    static class ReceivedVH extends RecyclerView.ViewHolder {
        TextView body, time;
        ReceivedVH(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.messageBody);
            time = itemView.findViewById(R.id.messageTime);
        }
        void bind(ChatMessage m) {
            body.setText(m.getText());
            time.setText(format(m.getTimestamp()));
        }
    }

    private static String format(long ts) {
        if (ts <= 0) return "";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(ts));
    }
}
