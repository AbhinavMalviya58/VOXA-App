package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerUserModelAdapter extends RecyclerView.Adapter<RecyclerUserModelAdapter.ViewHolder> {
    
    // Interface for item click events
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    
    private Context context;
    private ArrayList<userModel> userModelArrayList;
    private OnItemClickListener listener;
    
    public RecyclerUserModelAdapter(Context context, ArrayList<userModel> userModelArrayList, OnItemClickListener listener) {
        this.context = context;
        this.userModelArrayList = userModelArrayList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.user_posts, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.userPostProfileImg.setImageResource(userModelArrayList.get(position).userPostProfileImg);
        holder.userName.setText(userModelArrayList.get(position).userName);
        holder.userProfileBio.setText(userModelArrayList.get(position).userProfileBio);
        holder.userPostText.setText(userModelArrayList.get(position).userPostText);
        holder.userPostImg.setImageResource(userModelArrayList.get(position).userPostImg);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userModelArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userPostProfileImg, userPostImg;
        TextView userName, userProfileBio, userPostText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userPostProfileImg = itemView.findViewById(R.id.userPostProfileImg);
            userPostImg = itemView.findViewById(R.id.userPostImg);
            userName = itemView.findViewById(R.id.userName);
            userProfileBio = itemView.findViewById(R.id.userProfileBio);
            userPostText = itemView.findViewById(R.id.userPostText);
        }
    }
}
