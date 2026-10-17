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
 * Every entry is in one of three states:
 *
 *   WHITE  not mastered yet
 *   RED    the last redo was reported as wrong
 *   GREEN  mastered
 *
 * Transitions:
 *
 *   WHITE + correct -> GREEN      one correct redo is enough from white
 *   RED   + correct -> WHITE      then one more correct redo turns it green
 *   GREEN + correct -> GREEN
 *   any   + wrong   -> RED        a wrong redo always drops to red at once
 */
final class WrongBook {

    static final int STATE_WHITE = 0;
    static final int STATE_RED = 1;
    static final int STATE_GREEN = 2;

    private static final String PREFS = "detmatrix_wrong";

    /** Key of the single determinant collection. */
    static final String KEY_DET = "wrong_det";

    private WrongBook() {
    }

    static String keyForShape(int shapeIndex) {
        return "wrong_mm_" + shapeIndex;
    }

    /**
     * Next state after a redo. Deliberately pure and static so the rules can be
     * checked without a Context.
     */
    static int nextState(int current, boolean correct) {
        if (!correct) {
            return STATE_RED;
        }
        if (current == STATE_RED) {
            return STATE_WHITE;
        }
        return STATE_GREEN;
    }

    static int state(Item item) {
        return item.state;
    }

    /** One saved problem: A is m x k, B is k x n; B is empty in determinant mode. */
    static final class Item {
        final int m;
        final int k;
        final int n;
        final int[] a;
        final int[] b;
        /** One of {@link #STATE_WHITE}, {@link #STATE_RED}, {@link #STATE_GREEN}. */
        int state;

        Item(int m, int k, int n, int[] a, int[] b, int state) {
            this.m = m;
            this.k = k;
            this.n = n;
            this.a = a;
            this.b = b;
            this.state = state;
        }

        String encode() {
            StringBuilder sb = new StringBuilder();
            sb.append(m).append(',').append(k).append(',').append(n).append('|');
            join(sb, a);
            sb.append('|');
            join(sb, b);
            // "s" prefix so an old bare 0/1 "done" flag can be told apart.
            sb.append('|').append('s').append(state);
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
                return new Item(m, k, n, splitInts(parts[1]), splitInts(parts[2]),
                        parseState(parts[3].trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * Reads the state field, accepting older encodings:
     *   "s2"  current format
     *   "0"/"1"        old "done" flag (1 meant green)
     *   "streak,lastWrong"  old counter format
     */
    private static int parseState(String raw) {
        if (raw.length() == 0) {
            return STATE_WHITE;
        }
        try {
            if (raw.charAt(0) == 's') {
                return clampState(Integer.parseInt(raw.substring(1).trim()));
            }
            if (raw.indexOf(',') >= 0) {
                String[] p = raw.split(",");
                int streak = Integer.parseInt(p[0].trim());
                boolean lastWrong = p.length > 1 && "1".equals(p[1].trim());
                if (streak >= 2) {
                    return STATE_GREEN;
                }
                return lastWrong ? STATE_RED : STATE_WHITE;
            }
            return "1".equals(raw) ? STATE_GREEN : STATE_WHITE;
        } catch (NumberFormatException e) {
            return STATE_WHITE;
        }
    }

    private static int clampState(int v) {
        if (v == STATE_RED || v == STATE_GREEN) {
            return v;
        }
        return STATE_WHITE;
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
            if (it.state == STATE_GREEN) {
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
     * Record a correct redo and return the resulting state, so the caller can
     * tell the user whether the entry is green now.
     */
    static int markCorrect(Context c, String key, int index) {
        List<Item> items = list(c, key);
        if (index < 0 || index >= items.size()) {
            return STATE_WHITE;
        }
        Item it = items.get(index);
        it.state = nextState(it.state, true);
        save(c, key, items);
        return it.state;
    }

    /** Record a wrong redo: the entry goes red whatever it was before. */
    static void markWrongAttempt(Context c, String key, int index) {
        List<Item> items = list(c, key);
        if (index < 0 || index >= items.size()) {
            return;
        }
        Item it = items.get(index);
        it.state = nextState(it.state, false);
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
