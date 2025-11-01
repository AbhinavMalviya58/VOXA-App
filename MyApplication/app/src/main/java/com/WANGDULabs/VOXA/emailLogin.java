package com.WANGDULabs.VOXA;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import com.WANGDULabs.VOXA.ui.activities.loginTab;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class emailLogin extends loginTab {
    TextView login_email, login_password;
    Button login_button;
    ImageView google;
    FirebaseAuth mAuth;
    FirebaseDatabase database;
    FirebaseUser mUser;
    GoogleSignInClient gsc;
    GoogleSignInOptions gso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

//        login_email = findViewById(R.id.login_email);
//        login_password = findViewById(R.id.login_password);
//        login_button = findViewById(R.id.login_button);
//
//        login_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String loginPassword, loginEmail;
//                Toast.makeText(emailLogin.this, "hai", Toast.LENGTH_SHORT).show();
//            }
//        });
    }
}
