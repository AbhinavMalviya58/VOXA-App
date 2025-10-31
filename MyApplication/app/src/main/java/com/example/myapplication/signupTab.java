package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.example.myapplication.databinding.ActivityMainBinding;
import com.example.myapplication.ui.activities.MainActivity;
import com.example.myapplication.ui.activities.loginTab;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class signupTab extends AppCompatActivity {
    ActivityMainBinding binding;
    ImageView logo;

    Animation animation;
    TextView name, signup_email, signup_password, login_btn1st,signup_confirm;
    private CheckBox showPassword;
    Button signup_button;
    FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference reference;
    FirebaseUser mUser;
    private DatabaseReference databaseReference, dbref;

    GoogleSignInClient gsc;
    GoogleSignInOptions gso;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_tab);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
//        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();


        login_btn1st = findViewById(R.id.login_btn1st);
        signup_button = findViewById(R.id.signup_button);
        showPassword = findViewById(R.id.showPassword);
        name = findViewById(R.id.name);
        signup_email = findViewById(R.id.signup_email);
        signup_password = findViewById(R.id.signup_password);
        logo = findViewById(R.id.logo);

        animation = AnimationUtils.loadAnimation(signupTab.this,R.anim.imganim);
        logo.startAnimation(animation);
        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    signup_password.setTransformationMethod(null);
                }else {
                    signup_password.setTransformationMethod(new PasswordTransformationMethod());
                }
            }
        });

        signup_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String Name, Email, Password;
                Name = name.getText().toString();
                Email = signup_email.getText().toString();
                Password = signup_password.getText().toString();

                if(Name.isEmpty()|| Email.isEmpty() || Password.isEmpty()){
                    name.setError("Enter your name");
                    signup_email.setError("Email is compulsory");
                    signup_password.setError("please entre your password");
                    return;
                }if(TextUtils.isEmpty(Email)){

                    return;
                }if (TextUtils.isEmpty(Password)){

                    return;
                }else {
                mAuth.createUserWithEmailAndPassword(Email, Password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(signupTab.this, "User Created Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), loginTab.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(signupTab.this, "User already login", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                }
            }
        });

        login_btn1st.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(signupTab.this,loginTab.class));
                Animatoo.INSTANCE.animateSwipeRight(signupTab.this);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}