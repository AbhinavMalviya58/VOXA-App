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

import java.util.Random;

public class EvenOrOdd extends AppCompatActivity {
    private TextView numberText, resultText;
    private MaterialButton btnEven, btnOdd;
    private final Random random = new Random();
    private int currentNumber;
    private final FirebaseRepository repo = new FirebaseRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_even_or_odd);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        numberText = findViewById(R.id.numberText);
        resultText = findViewById(R.id.resultText);
        btnEven = findViewById(R.id.btnEven);
        btnOdd = findViewById(R.id.btnOdd);

        btnEven.setOnClickListener(v -> check(true));
        btnOdd.setOnClickListener(v -> check(false));

        nextRound();

        FooterController.bind(this, FooterController.Tab.GAMES);
    }

    private void nextRound() {
        currentNumber = random.nextInt(100) + 1;
        numberText.setText(String.valueOf(currentNumber));
        resultText.setText("Choose Even or Odd");
    }

    private void check(boolean guessEven) {
        boolean isEven = (currentNumber % 2 == 0);
        if (isEven == guessEven) {
            resultText.setText("Correct!");
            repo.addXp(5);
            repo.incrementGameWin("even_or_odd");
        } else {
            resultText.setText("Wrong :(");
            repo.resetStreak();
        }
        nextRound();
    }
}