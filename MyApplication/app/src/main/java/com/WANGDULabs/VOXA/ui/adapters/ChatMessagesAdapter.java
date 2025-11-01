package com.WANGDULabs.VOXA.ui.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.ChatMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.VH> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final String myUid;
    private final List<ChatMessage> items = new ArrayList<>();

    public ChatMessagesAdapter(String myUid) { this.myUid = myUid; }

    public void setItems(List<ChatMessage> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        ChatMessage m = items.get(position);
        return myUid != null && myUid.equals(m.getSenderId()) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SENT ? R.layout.item_message_sent : R.layout.item_message_received;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int p) {
        ChatMessage m = items.get(p);
        h.body.setText(m.getText());
        h.time.setText(formatTime(m.getTimestamp()));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView body; TextView time;
        VH(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.messageBody);
            time = itemView.findViewById(R.id.messageTime);
        }
    }

    private String formatTime(long ts) {
        if (ts <= 0) return "";
        return DateFormat.format("hh:mm a", new Date(ts)).toString();
    }
}
