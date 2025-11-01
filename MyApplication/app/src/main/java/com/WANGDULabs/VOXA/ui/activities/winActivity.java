package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.WANGDULabs.VOXA.R;

public class winActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_win);
        boolean win = getIntent().getBooleanExtra("win", false);
        int score = getIntent().getIntExtra("score", 0);
        int attempts = getIntent().getIntExtra("attempts", 0);
        int timeLeftSec = getIntent().getIntExtra("timeLeftSec", 0);
        int level = getIntent().getIntExtra("level", 1);
        int target = getIntent().getIntExtra("target", -1);
        TextView tv = findViewById(R.id.textView);
        String summary = "Result: " + (win ? "Win" : "Loss") + "\n" +
                "Score: " + score + "\n" +
                "Attempts: " + attempts + "\n" +
                "Time left: " + timeLeftSec + "s\n" +
                "Level: " + level + (win ? "" : "\nTarget: " + target);
        tv.setText(summary);
        Button back = findViewById(R.id.button2);
        back.setOnClickListener(v -> finish());
    }
}