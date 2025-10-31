package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.ui.activities.loginTab;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class profilePage extends AppCompatActivity {
    TextView shoEmail,shoName,user,FamilyName,profileName,Gender;
    ImageView userImg;
    Button logout_button;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseDatabase database;
    GoogleSignInClient gsc;
    GoogleSignInOptions gso;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
//        shoName = findViewById(R.id.shoName);
        shoEmail = findViewById(R.id.shoEmail);
        user = findViewById(R.id.user);
        logout_button = findViewById(R.id.logout_button);
        userImg = findViewById(R.id.userImg);
        Gender = findViewById(R.id.genderName);
        profileName = findViewById(R.id.profile);


        if(mUser == null){
            Intent intent = new Intent(getApplicationContext(), loginTab.class);
            startActivity(intent);
            finish();
        }else {
            shoEmail.setText(mUser.getEmail());
        }
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null){
            String userName = account.getDisplayName();
            String userEmail = account.getEmail();
            String personFamilyName = account.getGivenName();
            user.setText(userName);
//            shoName.setText(userName);
            shoEmail.setText(userEmail);

//            profileName.setText(personFamilyName);
        }

        database = FirebaseDatabase.getInstance();

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), loginTab.class);
                startActivity(intent);
                finish();
//                logout();
            }
        });

//        Toolbar
        Toolbar toolbar = findViewById(R.id.materialToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    void logout(){
        gsc.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                finish();
                startActivity(new Intent(profilePage.this, loginTab.class));
            }
        });
    }
}