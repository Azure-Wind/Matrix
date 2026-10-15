package com.example.detmatrix;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Recorded solving times, one independent list per mode, persisted in
 * SharedPreferences so they survive app restarts. Only the most recent
 * {@link #MAX_KEEP} entries are retained to bound storage growth.
 */
final class StatsStore {

    static final int MODE_DET = 0;
    static final int MODE_MATMUL = 1;

    private static final String PREFS = "detmatrix_stats";
    private static final int MAX_KEEP = 200;

    private StatsStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(int mode) {
        return mode == MODE_DET ? "det" : "matmul";
    }

    /** Append one result (milliseconds), dropping the oldest if over the cap. */
    static void record(Context c, int mode, long millis) {
        if (millis <= 0L) {
            return;
        }
        SharedPreferences p = prefs(c);
        String cur = p.getString(key(mode), "");
        StringBuilder sb = new StringBuilder();
        if (cur != null && cur.length() > 0) {
            String[] parts = cur.split(",");
            int start = Math.max(0, parts.length - (MAX_KEEP - 1));
            for (int i = start; i < parts.length; i++) {
                if (parts[i].length() > 0) {
                    sb.append(parts[i]).append(',');
                }
            }
        }
        sb.append(millis);
        p.edit().putString(key(mode), sb.toString()).apply();
    }

    /** Most recent n results, newest first. */
    static long[] recent(Context c, int mode, int n) {
        String cur = prefs(c).getString(key(mode), "");
        if (cur == null || cur.length() == 0) {
            return new long[0];
        }
        String[] parts = cur.split(",");
        int count = Math.min(n, parts.length);
        long[] out = new long[count];
        for (int i = 0; i < count; i++) {
            out[i] = parse(parts[parts.length - 1 - i]);
        }
        return out;
    }

    static int count(Context c, int mode) {
        String cur = prefs(c).getString(key(mode), "");
        if (cur == null || cur.length() == 0) {
            return 0;
        }
        int n = 0;
        for (String p : cur.split(",")) {
            if (p.length() > 0) {
                n++;
            }
        }
        return n;
    }

    /**
     * Average of the most recent n results; if fewer than n exist, the average
     * of what is available. Returns -1 when there is no record at all.
     */
    static long average(Context c, int mode, int n) {
        long[] r = recent(c, mode, n);
        if (r.length == 0) {
            return -1L;
        }
        long sum = 0L;
        for (long v : r) {
            sum += v;
        }
        return sum / r.length;
    }

    private static long parse(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
