package com.WANGDULabs.VOXA.ui.adapters;

import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.ChatMessage;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.VH> {
    public interface OnLongClick { void onReact(ChatMessage m); }

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final String myUid;
    private final String otherUid;
    private final List<ChatMessage> items = new ArrayList<>();
    private OnLongClick onLongClick;

    public ChatMessagesAdapter(String myUid, String otherUid) { this.myUid = myUid; this.otherUid = otherUid; }

    public void setItems(List<ChatMessage> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    public void setOnLongClick(OnLongClick l) { this.onLongClick = l; }

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
        boolean isText = !TextUtils.isEmpty(m.getText());
        if (h.body != null) {
            h.body.setVisibility(isText ? View.VISIBLE : View.GONE);
            if (isText) h.body.setText(m.getText());
        }
        if (h.image != null) {
            if (!TextUtils.isEmpty(m.getImageUrl())) {
                h.image.setVisibility(View.VISIBLE);
                Glide.with(h.image.getContext()).load(m.getImageUrl()).into(h.image);
            } else {
                h.image.setVisibility(View.GONE);
            }
        }
        if (h.time != null) h.time.setText(formatTime(m.getTimestamp()));

        if (h.reaction != null) {
            String rx = reactionsText(m);
            h.reaction.setVisibility(TextUtils.isEmpty(rx) ? View.GONE : View.VISIBLE);
            if (!TextUtils.isEmpty(rx)) h.reaction.setText(rx);
        }

        if (h.status != null) {
            String status = computeStatus(m);
            h.status.setText(status);
        }

        h.itemView.setOnLongClickListener(v -> {
            if (onLongClick != null) onLongClick.onReact(m);
            return true;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView body; TextView time; ImageView image; TextView reaction; TextView status;
        VH(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.messageBody);
            time = itemView.findViewById(R.id.messageTime);
            image = itemView.findViewById(R.id.messageImage);
            reaction = itemView.findViewById(R.id.reactionText);
            status = itemView.findViewById(R.id.messageStatus);
        }
    }

    private String formatTime(long ts) {
        if (ts <= 0) return "";
        return DateFormat.format("hh:mm a", new Date(ts)).toString();
    }

    private String reactionsText(ChatMessage m) {
        if (m.getReactions() == null || m.getReactions().isEmpty()) return null;
        Set<String> uniq = new HashSet<>(m.getReactions().values());
        StringBuilder sb = new StringBuilder();
        for (String e : uniq) { if (!TextUtils.isEmpty(e)) { if (sb.length()>0) sb.append(" "); sb.append(e); } }
        return sb.toString();
    }

    private String computeStatus(ChatMessage m) {
        if (otherUid == null) return "";
        if (m.getSeenBy() != null && m.getSeenBy().contains(otherUid)) return "Seen";
        if (m.getDeliveredTo() != null && m.getDeliveredTo().contains(otherUid)) return "Delivered";
        return "Sent";
    }
}
