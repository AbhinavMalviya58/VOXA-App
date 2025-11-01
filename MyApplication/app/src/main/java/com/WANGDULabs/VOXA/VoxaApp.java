package com.WANGDULabs.VOXA;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class VoxaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {
        }
    }
}
