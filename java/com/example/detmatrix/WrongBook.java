package com.example.detmatrix;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrong-answer book. The determinant mode has a single collection; matrix
 * multiplication keeps 27 collections, one per size combination. Each
 * collection is one SharedPreferences key holding ';'-separated entries.
 *
 * Every entry tracks how many redos it has got right in a row, which gives it
 * one of three visual states:
 *
 *   WHITE  never redone correctly yet, and the last redo was not wrong
 *   RED    the last redo was reported as wrong
 *   GREEN  mastered: two correct redos in a row
 *
 * A wrong redo drops straight back to RED, even from GREEN.
 */
final class WrongBook {

    static final int STATE_WHITE = 0;
    static final int STATE_RED = 1;
    static final int STATE_GREEN = 2;

    /** Consecutive correct redos required for GREEN. */
    static final int MASTERY_STREAK = 2;

    private static final String PREFS = "detmatrix_wrong";

    /** Key of the single determinant collection. */
    static final String KEY_DET = "wrong_det";

    private WrongBook() {
    }

    static String keyForShape(int shapeIndex) {
        return "wrong_mm_" + shapeIndex;
    }

    /**
     * Visual state for a (streak, lastWrong) pair. Deliberately pure and static
     * so the rules can be checked without a Context.
     */
    static int state(int streak, boolean lastWrong) {
        if (streak >= MASTERY_STREAK) {
            return STATE_GREEN;
        }
        if (lastWrong) {
            return STATE_RED;
        }
        return STATE_WHITE;
    }

    static int state(Item item) {
        return state(item.streak, item.lastWrong);
    }

    /** One saved problem: A is m x k, B is k x n; B is empty in determinant mode. */
    static final class Item {
        final int m;
        final int k;
        final int n;
        final int[] a;
        final int[] b;
        /** Consecutive correct redos, capped at {@link #MASTERY_STREAK}. */
        int streak;
        /** Whether the most recent redo was reported as wrong. */
        boolean lastWrong;

        Item(int m, int k, int n, int[] a, int[] b, int streak, boolean lastWrong) {
            this.m = m;
            this.k = k;
            this.n = n;
            this.a = a;
            this.b = b;
            this.streak = streak;
            this.lastWrong = lastWrong;
        }

        String encode() {
            StringBuilder sb = new StringBuilder();
            sb.append(m).append(',').append(k).append(',').append(n).append('|');
            join(sb, a);
            sb.append('|');
            join(sb, b);
            sb.append('|').append(streak).append(',').append(lastWrong ? 1 : 0);
            return sb.toString();
        }

        static Item decode(String s) {
            String[] parts = s.split("\\|", -1);
            if (parts.length < 4) {
                return null;
            }
            String[] d = parts[0].split(",");
            if (d.length < 3) {
                return null;
            }
            try {
                int m = Integer.parseInt(d[0].trim());
                int k = Integer.parseInt(d[1].trim());
                int n = Integer.parseInt(d[2].trim());
                int streak = 0;
                boolean lastWrong = false;
                // Older builds stored a single 0/1 "done" flag here.
                String[] st = parts[3].split(",");
                if (st.length >= 2) {
                    streak = Integer.parseInt(st[0].trim());
                    lastWrong = "1".equals(st[1].trim());
                } else if (st.length == 1 && "1".equals(st[0].trim())) {
                    streak = MASTERY_STREAK;
                }
                return new Item(m, k, n, splitInts(parts[1]), splitInts(parts[2]),
                        streak, lastWrong);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private static void join(StringBuilder sb, int[] v) {
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
    }

    private static int[] splitInts(String s) {
        if (s == null || s.length() == 0) {
            return new int[0];
        }
        String[] p = s.split(",");
        int[] out = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            try {
                out[i] = Integer.parseInt(p[i].trim());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static List<Item> list(Context c, String key) {
        List<Item> out = new ArrayList<Item>();
        String raw = prefs(c).getString(key, "");
        if (raw == null || raw.length() == 0) {
            return out;
        }
        for (String part : raw.split(";")) {
            if (part.length() == 0) {
                continue;
            }
            Item it = Item.decode(part);
            if (it != null) {
                out.add(it);
            }
        }
        return out;
    }

    static int count(Context c, String key) {
        return list(c, key).size();
    }

    /** How many entries in this collection are mastered (green). */
    static int masteredCount(Context c, String key) {
        int n = 0;
        for (Item it : list(c, key)) {
            if (state(it) == STATE_GREEN) {
                n++;
            }
        }
        return n;
    }

    /**
     * Add one wrong answer. A problem whose A and B are identical to an existing
     * entry is not added twice. Returns whether something was actually added.
     */
    static boolean add(Context c, String key, Item item) {
        List<Item> items = list(c, key);
        for (Item it : items) {
            if (same(it.a, item.a) && same(it.b, item.b)) {
                return false;
            }
        }
        items.add(item);
        save(c, key, items);
        return true;
    }

    /**
     * Record a correct redo. Returns the new streak so the caller can tell the
     * user whether one more is needed.
     */
    static int markCorrect(Context c, String key, int index) {
        List<Item> items = list(c, key);
        if (index < 0 || index >= items.size()) {
            return 0;
        }
        Item it = items.get(index);
        it.streak = Math.min(MASTERY_STREAK, it.streak + 1);
        it.lastWrong = false;
        save(c, key, items);
        return it.streak;
    }

    /** Record a wrong redo: the entry goes back to red whatever it was before. */
    static void markWrongAttempt(Context c, String key, int index) {
        List<Item> items = list(c, key);
        if (index < 0 || index >= items.size()) {
            return;
        }
        Item it = items.get(index);
        it.streak = 0;
        it.lastWrong = true;
        save(c, key, items);
    }

    private static void save(Context c, String key, List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item it : items) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(it.encode());
        }
        prefs(c).edit().putString(key, sb.toString()).apply();
    }

    private static boolean same(int[] x, int[] y) {
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < x.length; i++) {
            if (x[i] != y[i]) {
                return false;
            }
        }
        return true;
    }
}
