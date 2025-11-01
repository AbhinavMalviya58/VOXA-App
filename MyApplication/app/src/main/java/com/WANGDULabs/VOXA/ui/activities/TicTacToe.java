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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TicTacToe extends AppCompatActivity {
    private TextView statusText;
    private MaterialButton[] cells = new MaterialButton[9];
    private MaterialButton resetBtn;
    private final char[] board = new char[9];
    private boolean gameOver = false;
    private final FirebaseRepository repo = new FirebaseRepository();
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tic_tac_toe);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.statusText);
        cells[0] = findViewById(R.id.cell0);
        cells[1] = findViewById(R.id.cell1);
        cells[2] = findViewById(R.id.cell2);
        cells[3] = findViewById(R.id.cell3);
        cells[4] = findViewById(R.id.cell4);
        cells[5] = findViewById(R.id.cell5);
        cells[6] = findViewById(R.id.cell6);
        cells[7] = findViewById(R.id.cell7);
        cells[8] = findViewById(R.id.cell8);
        resetBtn = findViewById(R.id.resetBtn);

        for (int i = 0; i < 9; i++) {
            final int idx = i;
            cells[i].setOnClickListener(v -> onPlayerMove(idx));
        }
        resetBtn.setOnClickListener(v -> reset());
        reset();

        FooterController.bind(this, FooterController.Tab.GAMES);
    }

    private void reset() {
        for (int i = 0; i < 9; i++) {
            board[i] = ' ';
            cells[i].setText("");
            cells[i].setEnabled(true);
        }
        statusText.setText("Your turn");
        gameOver = false;
    }

    private void onPlayerMove(int idx) {
        if (gameOver || board[idx] != ' ') return;
        board[idx] = 'X';
        cells[idx].setText("X");
        if (checkWin('X')) {
            statusText.setText("You win!");
            gameOver = true;
            repo.addXp(10);
            repo.incrementGameWin("tic_tac_toe");
            disableBoard();
            return;
        }
        if (getAvailable().isEmpty()) {
            statusText.setText("Draw");
            gameOver = true;
            disableBoard();
            return;
        }
        cpuMove();
    }

    private void cpuMove() {
        int move = findWinningMove('O');
        if (move == -1) move = findWinningMove('X');
        if (move == -1) move = bestAvailable();
        if (move == -1) move = getRandomAvailable();
        board[move] = 'O';
        cells[move].setText("O");
        if (checkWin('O')) {
            statusText.setText("You lose");
            gameOver = true;
            repo.resetStreak();
            disableBoard();
            return;
        }
        if (getAvailable().isEmpty()) {
            statusText.setText("Draw");
            gameOver = true;
            disableBoard();
        } else {
            statusText.setText("Your turn");
        }
    }

    private void disableBoard() {
        for (MaterialButton b : cells) b.setEnabled(false);
    }

    private boolean checkWin(char p) {
        int[][] lines = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
        };
        for (int[] ln : lines) {
            if (board[ln[0]] == p && board[ln[1]] == p && board[ln[2]] == p) return true;
        }
        return false;
    }
    private List<Integer> getAvailable() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 9; i++) if (board[i] == ' ') list.add(i);
        return list;
    }
    private int getRandomAvailable() {
        List<Integer> avail = getAvailable();
        if (avail.isEmpty()) return -1;
        return avail.get(random.nextInt(avail.size()));
    }
    private int findWinningMove(char p) {
        for (int i : getAvailable()) {
            board[i] = p;
            boolean w = checkWin(p);
            board[i] = ' ';
            if (w) return i;
        }
        return -1;
    }
    private int bestAvailable() {
        if (board[4] == ' ') return 4; // center
        int[] corners = {0,2,6,8};
        for (int c : corners) if (board[c] == ' ') return c;
        return -1;
    }
}