// GameHubActivity.java
package com.WANGDULabs.VOXA.ui.activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.ui.adapters.GameAdapter;
import com.WANGDULabs.VOXA.data.Models.GameItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import androidx.appcompat.content.res.AppCompatResources;
import com.WANGDULabs.VOXA.profilePage;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GameHubActivity extends AppCompatActivity {
    private RecyclerView gamesRecyclerView;
    private GameAdapter gameAdapter;
    private List<GameItem> gameItems;
    private static final String PREFS_NAME = "GamePrefs";
    private SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_hub);
    prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

         // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle("Game Hub");
        }
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null && u.getPhotoUrl() != null) {
            Glide.with(this).asBitmap().load(u.getPhotoUrl()).circleCrop().into(new CustomTarget<Bitmap>() {
                @Override public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    toolbar.setNavigationIcon(new BitmapDrawable(getResources(), resource));
                }
                @Override public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
            });
        } else {
            toolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.bglogo));
        }
        toolbar.setNavigationOnClickListener(v -> startActivity(new Intent(this, profilePage.class)));

        gamesRecyclerView = findViewById(R.id.gamesRecyclerView);
        gamesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false));
        gamesRecyclerView.setHasFixedSize(true);

        setupGamesList();
        loadGameProgress();

        gameAdapter = new GameAdapter(this, gameItems);
        gamesRecyclerView.setAdapter(gameAdapter);

        com.WANGDULabs.VOXA.ui.navigation.FooterController.bind(this, com.WANGDULabs.VOXA.ui.navigation.FooterController.Tab.GAMES);
    }

    private void setupToolbar() {
    }

    private void loadGameProgress() {
    }
    private void setupGamesList() {
        gameItems = new ArrayList<>();

        // Add games with proper constructors
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
                RockPaperScissors.class
        ));

        gameItems.add(new GameItem(
                R.drawable.videogame_asset_24,
                "Even or Odd",
                "Guess parity",
                EvenOrOdd.class
        ));

        gameItems.add(new GameItem(
                R.drawable.ic_math_quiz,
                "Math Quiz",
                "Test your math skills",
                MathQuiz.class
        ));

        gameItems.add(new GameItem(
                R.drawable.ic_tic_tac_toe,
                "Tic Tac Toe",
                "Classic X and O",
                TicTacToe.class
        ));


        // Add other games similarly...

        // Set up adapter
        gameAdapter = new GameAdapter(this, gameItems);
        gamesRecyclerView.setAdapter(gameAdapter);
    }
//    private void setupGamesList() {
//        gameItems = new ArrayList<>();
//        gameItems.add(new GameItem(
//                "Guess the Number",
//                R.drawable.ic_guess_number,
//                "Test your guessing skills",
//                guessTheNumber.class,
//                R.color.game_guess_number,
//                prefs.getInt("gtn_highscore", 0)
//        ));
//        // Add other games...
//
//        // Create game items
//        List<GameItem> gameItems = new ArrayList<>();
//        gameItems.add(new GameItem(
//                R.drawable.ic_guess_number,
//                "Guess the Number",
//                "Find the hidden number",
//                guessTheNumber.class
//        ));
//        gameItems.add(new GameItem(
//                R.drawable.ic_rps,
//                "Rock Paper Scissors",
//                "Classic RPS game",
//                PlaceholderActivity.class // Will be implemented next
//        ));
//        gameItems.add(new GameItem(
//                R.drawable.ic_math_quiz,
//                "Math Quiz",
//                "Test your math skills",
//                PlaceholderActivity.class
//        ));
//        gameItems.add(new GameItem(
//                R.drawable.ic_tic_tac_toe,
//                "Tic Tac Toe",
//                "Classic X and O",
//                PlaceholderActivity.class
//        ));
//        gameAdapter = new GameAdapter(gameItems, this::onGameSelected);
//        gamesRecyclerView.setAdapter(gameAdapter);
//        gamesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
//    }
    private void onGameSelected(GameItem game) {
        // Add transition animation
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                findViewById(R.id.gameCard),
                "game_transition"
        );
        startActivity(new Intent(this, game.getGameClass()), options.toBundle());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Add slide animation when going back
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}