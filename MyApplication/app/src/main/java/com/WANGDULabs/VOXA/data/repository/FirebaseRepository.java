package com.WANGDULabs.VOXA.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FirebaseRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase db = FirebaseDatabase.getInstance();

    public DatabaseReference usersRef() {
        return db.getReference("users");
    }

    public DatabaseReference currentUserRef() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return null;
        return usersRef().child(user.getUid());
    }

    public void addXp(int amount) {
        DatabaseReference ref = currentUserRef();
        if (ref == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("xp", ServerValue.increment((long) amount));
        ref.updateChildren(updates);
    }

    public void incrementGameWin(String gameKey) {
        DatabaseReference ref = currentUserRef();
        if (ref == null) return;
        ref.child("gameStats").child(gameKey).setValue(ServerValue.increment(1));
        ref.child("totalWins").setValue(ServerValue.increment(1));
        ref.child("currentStreak").setValue(ServerValue.increment(1));
        // highestStreak update via transaction
        ref.child("currentStreak").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long current = snapshot.getValue(Long.class);
                final long currentVal = (current == null) ? 0L : current;
                ref.child("highestStreak").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap2) {
                        Long high = snap2.getValue(Long.class);
                        if (high == null || currentVal > high) {
                            ref.child("highestStreak").setValue(currentVal);
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        ref.child("lastPlayed").setValue(System.currentTimeMillis());
    }

    public void resetStreak() {
        DatabaseReference ref = currentUserRef();
        if (ref == null) return;
        ref.child("currentStreak").setValue(0);
    }

    public void updateMathQuizHighScore(int score) {
        DatabaseReference ref = currentUserRef();
        if (ref == null) return;
        ref.child("mathQuizHighScore").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long old = snapshot.getValue(Long.class);
                if (old == null || score > old) {
                    ref.child("mathQuizHighScore").setValue(score);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
