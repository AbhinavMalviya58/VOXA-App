package com.WANGDULabs.VOXA.data.Models;

public class GameItem {
    private final int iconResId;
    private final String title;
    private final String description;
    private final Class<?> activityClass;
    private final int colorResId;
    private final int highScore;

    public GameItem(int iconResId, String title, String description, Class<?> activityClass) {
        this(iconResId, title, description, activityClass, 0, 0);
    }

    public GameItem(int iconResId, String title, String description,
                    Class<?> activityClass, int colorResId, int highScore) {
        this.iconResId = iconResId;
        this.title = title;
        this.description = description;
        this.activityClass = activityClass;
        this.colorResId = colorResId;
        this.highScore = highScore;
    }

    // Getters
    public int getIconResId() { return iconResId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Class<?> getGameClass() { return activityClass; }
    public int getColorResId() { return colorResId; }
    public int getHighScore() { return highScore; }
}