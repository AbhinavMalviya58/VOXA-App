package com.WANGDULabs.VOXA.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.ui.navigation.FooterController;

public class MotorControlActivity extends AppCompatActivity {

    private TextView tvConnectionStatus;
    private Button btnStart, btnStop, btnSpeedUp, btnSpeedDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_motor_control);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSpeedUp = findViewById(R.id.btnSpeedUp);
        btnSpeedDown = findViewById(R.id.btnSpeedDown);

        btnStart.setOnClickListener(v -> startMotor());
        btnStop.setOnClickListener(v -> stopMotor());
        btnSpeedUp.setOnClickListener(v -> increaseSpeed());
        btnSpeedDown.setOnClickListener(v -> decreaseSpeed());

        // TODO: Implement Bluetooth/UART communication to motor controller

        FooterController.bind(this, FooterController.Tab.HOME);
    }

    private void startMotor() {
        // TODO: Send start command via Bluetooth/UART
        tvConnectionStatus.setText("Motor started");
    }

    private void stopMotor() {
        // TODO: Send stop command via Bluetooth/UART
        tvConnectionStatus.setText("Motor stopped");
    }

    private void increaseSpeed() {
        // TODO: Send speed increase command
    }

    private void decreaseSpeed() {
        // TODO: Send speed decrease command
    }
}