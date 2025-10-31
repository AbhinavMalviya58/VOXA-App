package com.example.myapplication.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.example.myapplication.R;
import com.example.myapplication.RecyclerUserModelAdapter;
import com.example.myapplication.userModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private final ArrayList<userModel> arrUserModel = new ArrayList<>();

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        if (mUser == null) {
            startActivity(new Intent(MainActivity.this, loginTab.class));
            finish();
            return;
        }

        // RecyclerView from included media.xml uses id `media`
        recyclerView = findViewById(R.id.media);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Seed sample items (existing user_posts layout expects these fields)
        arrUserModel.add(new userModel(R.drawable.logo, R.drawable.facebook, "Abhinav Malviya", "hai ", "@string/random_text"));
        arrUserModel.add(new userModel(R.drawable.logo, R.drawable.dp, "Abhinav Malviya", "hai ", "@string/random_text"));
        arrUserModel.add(new userModel(R.drawable.logo, R.drawable.google, "Abhinav Malviya", "hai ", "@string/random_text"));

        RecyclerUserModelAdapter adapter = new RecyclerUserModelAdapter(this, arrUserModel, position -> {
            // Placeholder feed item clicks
            Toast.makeText(MainActivity.this, "Item " + position, Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

        // Footer game button
        ImageView guessTheNumberHome = findViewById(R.id.guessTheNumberHome);
        if (guessTheNumberHome != null) {
            guessTheNumberHome.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Guess The Number", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, GameHubActivity.class));
                Animatoo.INSTANCE.animateSlideRight(MainActivity.this);
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Keep as-is; do not redirect if already on MainActivity
    }
}