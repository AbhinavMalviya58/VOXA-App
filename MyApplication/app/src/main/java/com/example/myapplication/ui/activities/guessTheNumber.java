package com.example.myapplication.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;

import java.util.Objects;
import java.util.Random;

public class guessTheNumber extends AppCompatActivity {

    private static final int BASE_RANGE = 100;
    private static final int RANGE_INCREMENT = 50; // per level
    private static final int MAX_ATTEMPTS = 10;
    private static final long ROUND_TIME_MS = 30_000L; // 30 seconds

    private int target;
    private int attempts;
    private int level;
    private int maxRange;

    private CountDownTimer timer;
    private long timeLeftMs;

    private Vibrator vibrator;
    private EditText etGuess;
    private TextView tvHint; // guessTheNumberTextView
    private TextView tvInfo; // 'number' TextView reused for range/timer info
    private Button btnGuess, btnReset;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess_the_number);

        etGuess = findViewById(R.id.guessTheNumber);
        tvHint = findViewById(R.id.guessTheNumberTextView);
        btnGuess = findViewById(R.id.guessTheNumberBtn);
        btnReset = findViewById(R.id.resetBtn);
        tvInfo = findViewById(R.id.number);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        prefs = getSharedPreferences("voxa_games", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.materialToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        level = Math.max(1, prefs.getInt("gtn_level", 1));
        startRound();

        btnGuess.setOnClickListener(v -> onGuess());
        btnReset.setOnClickListener(v -> startRound());
    }

    private void startRound() {
        // Reset state
        attempts = 0;
        maxRange = BASE_RANGE + (level - 1) * RANGE_INCREMENT;
        target = new Random().nextInt(maxRange) + 1;
        timeLeftMs = ROUND_TIME_MS;
        etGuess.setText("");
        tvHint.setText("I have selected a number between\n 1 and " + maxRange + ".\nTry to guess it.");
        btnGuess.setEnabled(true);

        // Start timer
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(ROUND_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMs = millisUntilFinished;
                long sec = millisUntilFinished / 1000L;
                tvInfo.setText("Time: " + sec + "s | Range: 1-" + maxRange);
            }

            @Override
            public void onFinish() {
                tvInfo.setText("Time: 0s | Range: 1-" + maxRange);
                endRound(false, 0);
            }
        }.start();
    }

    private void onGuess() {
        String s = etGuess.getText().toString().trim();
        if (s.isEmpty()) {
            etGuess.setError("Empty");
            Toast.makeText(this, "Number is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        int guess;
        try {
            guess = Integer.parseInt(s);
        } catch (Exception e) {
            etGuess.setError("Invalid number");
            return;
        }
        if (guess < 1 || guess > maxRange) {
            tvHint.setText("Invalid: enter 1-" + maxRange);
            shake(etGuess);
            return;
        }

        attempts++;

        if (guess == target) {
            // Score formula
            int timeBonus = (int) (timeLeftMs / 1000L);
            int attemptBonus = Math.max(0, (MAX_ATTEMPTS - attempts)) * 5;
            int levelBonus = level * 10;
            int score = 50 + timeBonus + attemptBonus + levelBonus;
            endRound(true, score);
            return;
        }

        // Hints
        String hint = (guess < target) ? "Too low" : "Too high";
        int diff = Math.abs(guess - target);
        if (diff <= 5) hint += " • Very close";
        else if (diff <= 15) hint += " • Getting warmer";
        else hint += " • Keep trying";
        tvHint.setText(hint);
        shake(etGuess);

        if (attempts >= MAX_ATTEMPTS) {
            endRound(false, 0);
        }
    }

    private void endRound(boolean win, int score) {
        btnGuess.setEnabled(false);
        if (timer != null) timer.cancel();

        // Streak & XP in SharedPreferences (Firebase integration can be added later)
        int streak = prefs.getInt("gtn_streak", 0);
        int xp = prefs.getInt("xp", 0);
        if (win) {
            streak += 1;
            xp += score;
            level += 1; // progressive difficulty on win
            prefs.edit()
                    .putInt("gtn_streak", streak)
                    .putInt("xp", xp)
                    .putInt("gtn_level", level)
                    .apply();
            tvHint.setText("Congratulations! +" + score + " XP");
        } else {
            streak = 0;
            prefs.edit().putInt("gtn_streak", 0).apply();
            tvHint.setText("You lost. The number was " + target);
        }

        // Navigate to winActivity with summary
        Intent i = new Intent(this, winActivity.class);
        i.putExtra("win", win);
        i.putExtra("score", score);
        i.putExtra("attempts", attempts);
        i.putExtra("timeLeftSec", (int) (timeLeftMs / 1000L));
        i.putExtra("level", level);
        i.putExtra("target", target);
        startActivity(i);
//        Animatoo.INSTANCE.animateSlideRight(this);
    }

    private void shake(View v) {
        // light vibration + subtle shake
        try { if (vibrator != null) vibrator.vibrate(20); } catch (Exception ignored) {}
        v.animate()
                .translationXBy(20)
                .setDuration(50)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> v.animate().translationXBy(-40).setDuration(80)
                        .withEndAction(() -> v.animate().translationXBy(20).setDuration(50)))
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}