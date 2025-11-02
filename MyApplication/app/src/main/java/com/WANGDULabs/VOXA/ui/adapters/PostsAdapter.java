package com.WANGDULabs.VOXA.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.remote.VoxApi;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private final Context context;
    private final List<JSONObject> posts;

    public PostsAdapter(Context context, List<JSONObject> posts) {
        this.context = context;
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        JSONObject post = posts.get(position);
        try {
            JSONObject author = post.getJSONObject("author");
            holder.tvAuthor.setText(author.optString("displayName", "Unknown"));
            holder.tvText.setText(post.optString("text", ""));
            holder.tvLikes.setText(post.optInt("likesCount", 0) + " likes");
            holder.tvComments.setText(post.optInt("commentsCount", 0) + " comments");
            String photoUrl = author.optString("photoUrl");
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(context).load(photoUrl).circleCrop().into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.default_avatar);
            }
            // Like toggle
            holder.btnLike.setOnClickListener(v -> {
                try {
                    VoxApi.likePost(post.getString("id"))
                            .addOnSuccessListener(result -> {
                                try {
                                    boolean liked = result.getBoolean("liked");
                                    holder.btnLike.setTextColor(liked ? context.getColor(R.color.accent_blue) : context.getColor(R.color.text_secondary));
                                    // Update count
                                    int newCount = post.optInt("likesCount", 0) + (liked ? 1 : -1);
                                    post.put("likesCount", newCount);
                                    holder.tvLikes.setText(newCount + " likes");
                                } catch (JSONException e) {
                                    // ignore
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Optionally show error
                            });
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (JSONException e) {
            // Skip malformed item
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvAuthor, tvText, tvLikes, tvComments;
        TextView btnLike, btnComment;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvText = itemView.findViewById(R.id.tvText);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvComments = itemView.findViewById(R.id.tvComments);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }
}
