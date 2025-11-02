package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;

public class VibeActivity extends AppCompatActivity {

    private RecyclerView vibeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vibe);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        vibeList = findViewById(R.id.vibeList);
        if (vibeList != null) {
            vibeList.setLayoutManager(new LinearLayoutManager(this));
            // TODO: Load curated vibe items from Firestore collection 'vibe'
        }

        FooterController.bind(this, FooterController.Tab.VIBE);
    }
}