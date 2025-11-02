package com.WANGDULabs.VOXA.ui.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.ChatListItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.VH> {
    public interface OnClick { void onItem(ChatListItem item); }

    private final List<ChatListItem> items = new ArrayList<>();
    private final OnClick onClick;

    public ChatListAdapter(OnClick onClick) { this.onClick = onClick; }

    public void setItems(List<ChatListItem> data, boolean append) {
        if (!append) items.clear();
        int start = items.size();
        items.addAll(data);
        if (append) notifyItemRangeInserted(start, data.size());
        else notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int p) {
        ChatListItem item = items.get(p);
        h.name.setText(item.getDisplayName());
        h.lastMessage.setText(item.getLastMessage() == null ? "" : item.getLastMessage());
        if (item.getPhotoUrl() != null && !item.getPhotoUrl().isEmpty())
            Glide.with(h.itemView.getContext()).load(item.getPhotoUrl()).circleCrop().into(h.avatar);
        if (h.time != null) h.time.setText(formatTime(item.getUpdatedAt()));
        h.itemView.setOnClickListener(v -> onClick.onItem(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar; TextView name; TextView lastMessage; TextView time;
        VH(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            time = itemView.findViewById(R.id.time);
        }
    }

    private String formatTime(long ts) {
        if (ts <= 0) return "";
        return DateFormat.format("hh:mm a", new Date(ts)).toString();
    }
}
