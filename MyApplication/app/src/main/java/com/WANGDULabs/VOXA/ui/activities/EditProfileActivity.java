package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etDisplayName, etHandle, etEmail, etBio;
    private Switch isPrivateSwitch;
    private Button btnSave;
    private ImageView ivAvatar, ivBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etDisplayName = findViewById(R.id.etDisplayName);
        etHandle = findViewById(R.id.etHandle);
        etEmail = findViewById(R.id.etEmail);
        etBio = findViewById(R.id.etBio);
        isPrivateSwitch = findViewById(R.id.isPrivateSwitch);
        btnSave = findViewById(R.id.btnSave);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivBanner = findViewById(R.id.ivBanner);

        btnSave.setOnClickListener(v -> saveProfile());
        ivAvatar.setOnClickListener(v -> pickAvatar());
        ivBanner.setOnClickListener(v -> pickBanner());

        // TODO: Load current profile data into fields

        FooterController.bind(this, FooterController.Tab.HOME);
    }

    private void saveProfile() {
        // TODO: Validate and write to Firestore under users/{uid}
    }

    private void pickAvatar() {
        // TODO: Launch image picker and upload to Firebase Storage (profile_images/{uid})
    }

    private void pickBanner() {
        // TODO: Launch image picker and upload to Firebase Storage (banners/{uid})
    }
}