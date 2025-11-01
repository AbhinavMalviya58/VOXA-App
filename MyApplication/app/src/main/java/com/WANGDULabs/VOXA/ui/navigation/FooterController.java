package com.WANGDULabs.VOXA.ui.navigation;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import com.WANGDULabs.VOXA.R;
import com.WANGDULabs.VOXA.notifications;
import com.WANGDULabs.VOXA.ui.activities.GameHubActivity;
import com.WANGDULabs.VOXA.ui.activities.MainActivity;
import com.WANGDULabs.VOXA.PlaceholderActivity;

public class FooterController {

    public enum Tab { HOME, VIBE, VOX, GAMES, NOTIF }

    public static void bind(Activity activity, Tab current) {
        View home = activity.findViewById(R.id.navHome);
        View vibe = activity.findViewById(R.id.navVibe);
        View vox = activity.findViewById(R.id.navVox);
        View games = activity.findViewById(R.id.navGames);
        View notif = activity.findViewById(R.id.navNotif);

        ImageView imgHome = activity.findViewById(R.id.imgHome);
        ImageView imgVibe = activity.findViewById(R.id.imgVibe);
        ImageView imgVox = activity.findViewById(R.id.imgVox);
        ImageView imgGames = activity.findViewById(R.id.imgGames);
        ImageView imgNotif = activity.findViewById(R.id.imgNotif);

        if (imgHome != null) imgHome.setSelected(current == Tab.HOME);
        if (imgVibe != null) imgVibe.setSelected(current == Tab.VIBE);
        if (imgVox != null) imgVox.setSelected(current == Tab.VOX);
        if (imgGames != null) imgGames.setSelected(current == Tab.GAMES);
        if (imgNotif != null) imgNotif.setSelected(current == Tab.NOTIF);

        if (home != null) home.setOnClickListener(v -> navigateWithPulse(activity, v, current != Tab.HOME, MainActivity.class));
        if (vibe != null) vibe.setOnClickListener(v -> navigateWithPulse(activity, v, current != Tab.VIBE, PlaceholderActivity.class));
        if (vox != null) vox.setOnClickListener(v -> navigateWithPulse(activity, v, current != Tab.VOX, PlaceholderActivity.class));
        if (games != null) games.setOnClickListener(v -> navigateWithPulse(activity, v, current != Tab.GAMES, GameHubActivity.class));
        if (notif != null) notif.setOnClickListener(v -> navigateWithPulse(activity, v, current != Tab.NOTIF, notifications.class));
    }

    private static void navigateWithPulse(Activity activity, View v, boolean shouldNavigate, Class<?> target) {
        if (!shouldNavigate) return;
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
                .withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    activity.startActivity(new Intent(activity, target));
                }).start();
    }
}
