// PlaceholderActivity.java
package com.WANGDULabs.VOXA;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.WANGDULabs.VOXA.R;

public class PlaceholderActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder);

        TextView textView = findViewById(R.id.placeholderText);
        String className = getIntent().getComponent().getShortClassName();
        textView.setText(className.substring(1) + " coming soon!");
    }
}