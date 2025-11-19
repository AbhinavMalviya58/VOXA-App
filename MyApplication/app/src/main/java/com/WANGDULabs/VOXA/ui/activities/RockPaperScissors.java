package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;
import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.repository.FirebaseRepository;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;
import com.WANGDULabs.VOXA.data.multiplayer.RoomManager;
import com.WANGDULabs.VOXA.data.multiplayer.MoveManager;
import com.WANGDULabs.VOXA.data.multiplayer.ResultManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

public class RockPaperScissors extends AppCompatActivity {
    private MaterialButton btnRock, btnPaper, btnScissors;
    private TextView resultText;
    private final FirebaseRepository repo = new FirebaseRepository();

    private enum Move { ROCK, PAPER, SCISSORS }

    private MaterialButton btnGenerateCode, btnJoinRoom;
    private EditText edtRoomCode;
    private TextView txtRoomStatus;
    private final RoomManager roomManager = new RoomManager();
    private final MoveManager moveManager = new MoveManager();
    private final ResultManager resultManager = new ResultManager();
    private String roomId = null;
    private boolean multiplayer = false;

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
        btnGenerateCode = findViewById(R.id.btnGenerateCode);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        edtRoomCode = findViewById(R.id.edtRoomCode);
        txtRoomStatus = findViewById(R.id.txtRoomStatus);

        btnRock.setOnClickListener(v -> play(Move.ROCK));
        btnPaper.setOnClickListener(v -> play(Move.PAPER));
        btnScissors.setOnClickListener(v -> play(Move.SCISSORS));

        btnGenerateCode.setOnClickListener(v -> {
            btnGenerateCode.setEnabled(false);
            roomManager.createRoom("RPS", (ok, value, e) -> runOnUiThread(() -> {
                btnGenerateCode.setEnabled(true);
                if (ok) {
                    roomId = value;
                    multiplayer = true;
                    txtRoomStatus.setText("Room: " + roomId + " (waiting for opponent)");
                    disableChoices(true);
                    listenRoom();
                    listenResult();
                } else {
                    txtRoomStatus.setText("Failed to create room");
                }
            }));
        });

        btnJoinRoom.setOnClickListener(v -> {
            String code = edtRoomCode.getText() != null ? edtRoomCode.getText().toString().trim() : "";
            if (code.length() < 6) { txtRoomStatus.setText("Enter valid code"); return; }
            btnJoinRoom.setEnabled(false);
            roomManager.joinRoom(code, (ok, joined, e) -> runOnUiThread(() -> {
                btnJoinRoom.setEnabled(true);
                if (ok) {
                    roomId = code;
                    multiplayer = true;
                    txtRoomStatus.setText("Joined room: " + roomId);
                    disableChoices(true);
                    listenRoom();
                    listenResult();
                } else {
                    txtRoomStatus.setText("Join failed");
                }
            }));
        });

        FooterController.bind(this, FooterController.Tab.GAMES);
    }

    private void play(Move player) {
        if (multiplayer && roomId != null) {
            try {
                JSONObject move = new JSONObject();
                String mv = player == Move.ROCK ? "rock" : (player == Move.PAPER ? "paper" : "scissors");
                move.put("move", mv);
                resultText.setText("Waiting for opponent...");
                disableChoices(true);
                moveManager.submitMove(roomId, move);
            } catch (JSONException ignored) {}
            return;
        }
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

    private void listenRoom() {
        if (roomId == null) return;
        roomManager.listenRoom(roomId, (DocumentSnapshot value, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
            if (value == null || !value.exists()) return;
            String status = value.getString("status");
            String opp = value.getString("opponentUid");
            runOnUiThread(() -> {
                if ("playing".equals(status) && opp != null) {
                    txtRoomStatus.setText("Opponent joined");
                    disableChoices(false);
                } else if ("waiting".equals(status)) {
                    txtRoomStatus.setText("Room: " + roomId + " (waiting for opponent)");
                    disableChoices(true);
                } else if ("finished".equals(status)) {
                    disableChoices(true);
                }
            });
        });
    }

    private void listenResult() {
        if (roomId == null) return;
        resultManager.listenResult(roomId, (DocumentSnapshot value, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
            if (value == null || !value.exists()) return;
            String winnerUid = value.getString("winnerUid");
            String my = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            runOnUiThread(() -> {
                if (winnerUid == null) {
                    resultText.setText("Draw");
                } else if (winnerUid.equals(my)) {
                    resultText.setText("You Win!");
                } else {
                    resultText.setText("You Lose!");
                }
                disableChoices(true);
            });
        });
    }

    private void disableChoices(boolean disabled) {
        btnRock.setEnabled(!disabled);
        btnPaper.setEnabled(!disabled);
        btnScissors.setEnabled(!disabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        roomManager.stopListening();
        moveManager.stopListening();
        resultManager.stopListening();
    }
}