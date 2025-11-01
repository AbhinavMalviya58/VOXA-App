package com.WANGDULabs.VOXA.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.SearchUser;

import java.util.ArrayList;
import java.util.List;

public class FollowUserAdapter extends RecyclerView.Adapter<FollowUserAdapter.VH> {
    public interface OnClick { void onUser(SearchUser u); }
    public interface OnToggle { void onToggle(SearchUser u); }
    private final Context ctx;
    private final List<SearchUser> items = new ArrayList<>();
    private final OnClick onClick;
    private final OnToggle onToggle;

    public FollowUserAdapter(Context ctx, OnClick onClick, OnToggle onToggle) {
        this.ctx = ctx; this.onClick = onClick; this.onToggle = onToggle;
    }

    public void setItems(List<SearchUser> data, boolean append) {
        if (!append) items.clear();
        int start = items.size();
        items.addAll(data);
        if (append) notifyItemRangeInserted(start, data.size());
        else notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follow_user, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int p) {
        SearchUser u = items.get(p);
        h.name.setText(u.getDisplayName());
        h.handle.setText("@" + u.getUserId_lc());
        if (u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) Glide.with(ctx).load(u.getPhotoUrl()).circleCrop().into(h.avatar);
        h.itemView.setOnClickListener(v -> onClick.onUser(u));
        h.btnFollowToggle.setOnClickListener(v -> onToggle.onToggle(u));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar; TextView name; TextView handle; Button btnFollowToggle;
        VH(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            handle = itemView.findViewById(R.id.handle);
            btnFollowToggle = itemView.findViewById(R.id.btnFollowToggle);
        }
    }
}
