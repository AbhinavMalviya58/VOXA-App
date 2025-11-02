package com.WANGDULabs.VOXA.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.data.remote.VoxApi;
import com.google.android.material.card.MaterialCardView;

public class PostCreationActivity extends AppCompatActivity {

    private EditText etPostText;
    private ImageView ivMediaPreview;
    private Button btnPost, btnAttach;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                selectedImageUri = uri;
                ivMediaPreview.setImageURI(uri);
                ivMediaPreview.setVisibility(View.VISIBLE);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_creation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etPostText = findViewById(R.id.etPostText);
        ivMediaPreview = findViewById(R.id.ivMediaPreview);
        btnPost = findViewById(R.id.btnPost);
        btnAttach = findViewById(R.id.btnAttach);

        btnAttach.setOnClickListener(v -> pickImage.launch("image/*"));
        btnPost.setOnClickListener(v -> submitPost());
    }

    private void submitPost() {
        String text = etPostText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Post cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        // For simplicity, we only handle text posts here. Media upload can be added later.
        VoxApi.createPost(text)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Posted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
