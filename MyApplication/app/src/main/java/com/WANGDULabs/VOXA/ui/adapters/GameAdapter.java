package com.WANGDULabs.VOXA.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.Models.GameItem;

import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {
    private final Context context;
    private final List<GameItem> gameItems;
    private int lastPosition = -1;
    private List<GameItem> games;
//    private OnGameClickListener listener;



    public GameAdapter(Context context, List<GameItem> gameItems) {
        this.context = context;
        this.gameItems = gameItems;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_game_card, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameItem item = gameItems.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
        setAnimation(holder.itemView, position);
        holder.cardView.setOnClickListener(v -> {
            context.startActivity(new Intent(context, item.getGameClass()));
            Animatoo.INSTANCE.animateSlideLeft(context);
        });
    }
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            viewToAnimate.setAlpha(0f);
            viewToAnimate.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(position * 100)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            lastPosition = position;
        }
    }
    @Override
    public int getItemCount() {
        return gameItems.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView icon;
        TextView title, description;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.gameCard);
            icon = itemView.findViewById(R.id.gameIcon);
            title = itemView.findViewById(R.id.gameTitle);
            description = itemView.findViewById(R.id.gameDescription);
        }
    }
}