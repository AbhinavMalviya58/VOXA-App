package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.repository.FirebaseRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class MathQuiz extends AppCompatActivity {
    private TextView questionText, scoreText;
    private EditText answerInput;
    private MaterialButton submitBtn;
    private int a, b, answer;
    private final Random random = new Random();
    private int score = 0;
    private int high = 0;
    private final FirebaseRepository repo = new FirebaseRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_math_quiz);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        questionText = findViewById(R.id.questionText);
        scoreText = findViewById(R.id.scoreText);
        answerInput = findViewById(R.id.answerInput);
        submitBtn = findViewById(R.id.submitBtn);

        
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) {
            FirebaseDatabase.getInstance().getReference("users").child(u.getUid()).child("mathQuizHighScore")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(DataSnapshot snapshot) {
                            Long val = snapshot.getValue(Long.class);
                            high = val != null ? val.intValue() : 0;
                            updateScoreText();
                        }
                        @Override public void onCancelled(DatabaseError error) { }
                    });
        }

        nextQuestion();
        submitBtn.setOnClickListener(v -> submit());
    }

    private void nextQuestion() {
        a = random.nextInt(20) + 1;
        b = random.nextInt(20) + 1;
        int op = random.nextInt(3);
        switch (op) {
            case 0:
                answer = a + b;
                questionText.setText(a + " + " + b + " = ?");
                break;
            case 1:
                
                if (a < b) { int t = a; a = b; b = t; }
                answer = a - b;
                questionText.setText(a + " - " + b + " = ?");
                break;
            default:
                answer = a * b;
                questionText.setText(a + " × " + b + " = ?");
        }
        answerInput.setText("");
    }

    private void submit() {
        String t = answerInput.getText().toString().trim();
        if (TextUtils.isEmpty(t)) return;
        int guess;
        try { guess = Integer.parseInt(t); } catch (Exception e) { return; }
        if (guess == answer) {
            score += 10;
            repo.addXp(10);
            if (score > high) {
                high = score;
                repo.updateMathQuizHighScore(high);
            }
            updateScoreText();
            nextQuestion();
        } else {
            
            repo.resetStreak();
            score = Math.max(0, score - 5);
            updateScoreText();
        }
    }

    private void updateScoreText() {
        scoreText.setText("Score: " + score + "  High: " + high);
    }
}