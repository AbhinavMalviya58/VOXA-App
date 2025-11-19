package com.WANGDULabs.VOXA.data.multiplayer;

import android.os.CountDownTimer;

public class GameTimer {
    public interface Listener {
        void onTick(long millisRemaining);
        void onFinish();
    }

    private CountDownTimer timer;

    public void start(long durationMs, long intervalMs, Listener listener) {
        cancel();
        timer = new CountDownTimer(durationMs, intervalMs) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (listener != null) listener.onTick(millisUntilFinished);
            }
            @Override
            public void onFinish() {
                if (listener != null) listener.onFinish();
            }
        };
        timer.start();
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
