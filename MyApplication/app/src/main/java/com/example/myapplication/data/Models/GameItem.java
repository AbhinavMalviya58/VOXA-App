package com.example.myapplication.data.Models;

public class GameItem {
    private final int iconResId;
    private final String title;
    private final String description;
    private final Class<?> activityClass;

    public GameItem(int iconResId, String title, String description, Class<?> activityClass) {
        this.iconResId = iconResId;
        this.title = title;
        this.description = description;
        this.activityClass = activityClass;
    }

    // Getters
    public int getIconResId() { return iconResId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Class<?> getActivityClass() { return activityClass; }
}