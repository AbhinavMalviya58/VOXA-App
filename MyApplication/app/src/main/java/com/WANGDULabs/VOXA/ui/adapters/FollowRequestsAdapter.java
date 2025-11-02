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

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.SearchUser;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class FollowRequestsAdapter extends RecyclerView.Adapter<FollowRequestsAdapter.VH> {
    public interface OnApprove { void onApprove(SearchUser u); }
    public interface OnDeny { void onDeny(SearchUser u); }

    private final Context ctx;
    private final List<SearchUser> items = new ArrayList<>();
    private final OnApprove onApprove;
    private final OnDeny onDeny;

    public FollowRequestsAdapter(Context ctx, OnApprove onApprove, OnDeny onDeny) {
        this.ctx = ctx; this.onApprove = onApprove; this.onDeny = onDeny;
    }

    public void setItems(List<SearchUser> data, boolean append) {
        if (!append) items.clear();
        int start = items.size();
        items.addAll(data);
        if (append) notifyItemRangeInserted(start, data.size());
        else notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follow_request, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int p) {
        SearchUser u = items.get(p);
        h.name.setText(u.getDisplayName());
        h.handle.setText("@" + u.getUserId_lc());
        if (u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) Glide.with(ctx).load(u.getPhotoUrl()).circleCrop().into(h.avatar);
        h.btnApprove.setOnClickListener(v -> onApprove.onApprove(u));
        h.btnDeny.setOnClickListener(v -> onDeny.onDeny(u));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar; TextView name; TextView handle; Button btnApprove; Button btnDeny;
        VH(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            handle = itemView.findViewById(R.id.handle);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDeny = itemView.findViewById(R.id.btnDeny);
        }
    }
}
