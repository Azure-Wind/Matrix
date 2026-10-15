package com.example.detmatrix;

import android.os.Handler;
import android.os.Looper;

/**
 * Handler-based stopwatch that reports the elapsed milliseconds to a listener at
 * a fixed interval. Shared by the determinant and matrix-multiplication screens.
 */
final class ElapsedTimer {

    interface Listener {
        void onElapsed(long elapsedMillis);
    }

    private static final long TICK_MS = 50L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            elapsed = System.currentTimeMillis() - startTime;
            listener.onElapsed(elapsed);
            handler.postDelayed(this, TICK_MS);
        }
    };

    private long startTime;
    private long elapsed;
    private boolean running;

    ElapsedTimer(Listener listener) {
        this.listener = listener;
    }

    /** Start counting from zero. */
    void start() {
        startFrom(0L);
    }

    /** Resume counting from an already accumulated number of milliseconds. */
    void startFrom(long alreadyElapsedMillis) {
        handler.removeCallbacks(ticker);
        elapsed = alreadyElapsedMillis;
        startTime = System.currentTimeMillis() - alreadyElapsedMillis;
        running = true;
        listener.onElapsed(elapsed);
        handler.post(ticker);
    }

    /** Stop counting, keeping the current reading. */
    void stop() {
        handler.removeCallbacks(ticker);
        if (running) {
            elapsed = System.currentTimeMillis() - startTime;
            running = false;
        }
        listener.onElapsed(elapsed);
    }

    /** Stop and set the reading to a given value (used when restoring UI state). */
    void setElapsed(long value) {
        handler.removeCallbacks(ticker);
        running = false;
        elapsed = value;
        listener.onElapsed(elapsed);
    }

    /** Reset to zero and stop. */
    void reset() {
        setElapsed(0L);
    }

    long elapsed() {
        return running ? System.currentTimeMillis() - startTime : elapsed;
    }

    boolean isRunning() {
        return running;
    }

    /** Cancel pending callbacks without notifying the listener (used in onDestroy). */
    void cancel() {
        handler.removeCallbacks(ticker);
    }
}
