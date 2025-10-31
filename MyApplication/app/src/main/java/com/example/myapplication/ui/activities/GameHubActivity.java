// GameHubActivity.java
package com.example.myapplication.ui.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.PlaceholderActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.adapters.GameAdapter;
import com.example.myapplication.data.Models.GameItem;
import com.example.myapplication.ui.activities.guessTheNumber;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class GameHubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_hub);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup RecyclerView with grid layout
        RecyclerView recyclerView = findViewById(R.id.gamesRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setHasFixedSize(true);

        // Create game items
        List<GameItem> gameItems = new ArrayList<>();
        gameItems.add(new GameItem(
                R.drawable.ic_guess_number,
                "Guess the Number",
                "Find the hidden number",
                guessTheNumber.class
        ));
        gameItems.add(new GameItem(
                R.drawable.ic_rps,
                "Rock Paper Scissors",
                "Classic RPS game",
                PlaceholderActivity.class // Will be implemented next
        ));
        gameItems.add(new GameItem(
                R.drawable.ic_math_quiz,
                "Math Quiz",
                "Test your math skills",
                PlaceholderActivity.class
        ));
        gameItems.add(new GameItem(
                R.drawable.ic_tic_tac_toe,
                "Tic Tac Toe",
                "Classic X and O",
                PlaceholderActivity.class
        ));

        // Set adapter
        GameAdapter adapter = new GameAdapter(this, gameItems);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Add slide animation when going back
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}