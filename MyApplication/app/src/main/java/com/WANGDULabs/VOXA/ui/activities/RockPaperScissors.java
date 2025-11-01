package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.repository.FirebaseRepository;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;

public class RockPaperScissors extends AppCompatActivity {
    private MaterialButton btnRock, btnPaper, btnScissors;
    private TextView resultText;
    private final FirebaseRepository repo = new FirebaseRepository();

    private enum Move { ROCK, PAPER, SCISSORS }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rock_paper_scissors);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnRock = findViewById(R.id.btnRock);
        btnPaper = findViewById(R.id.btnPaper);
        btnScissors = findViewById(R.id.btnScissors);
        resultText = findViewById(R.id.resultText);

        btnRock.setOnClickListener(v -> play(Move.ROCK));
        btnPaper.setOnClickListener(v -> play(Move.PAPER));
        btnScissors.setOnClickListener(v -> play(Move.SCISSORS));

        FooterController.bind(this, FooterController.Tab.GAMES);
    }

    private void play(Move player) {
        Move cpu = randomMove();
        String outcome;
        int xp = 0;
        if (player == cpu) {
            outcome = "Tie!";
        } else if (wins(player, cpu)) {
            outcome = "You Win!";
            xp = 10;
            repo.incrementGameWin("rock_paper_scissors");
        } else {
            outcome = "You Lose!";
            repo.resetStreak();
        }
        if (xp > 0) repo.addXp(xp);
        resultText.setText(outcome + " (You: " + player.name() + ", CPU: " + cpu.name() + ")");
    }

    private boolean wins(Move a, Move b) {
        return (a == Move.ROCK && b == Move.SCISSORS) ||
               (a == Move.PAPER && b == Move.ROCK) ||
               (a == Move.SCISSORS && b == Move.PAPER);
    }

    private Move randomMove() {
        int r = (int) (Math.random() * 3);
        if (r == 0) return Move.ROCK;
        if (r == 1) return Move.PAPER;
        return Move.SCISSORS;
    }
}