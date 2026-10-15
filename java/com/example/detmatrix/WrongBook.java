package com.example.detmatrix;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrong-answer book. The determinant mode has a single collection; matrix
 * multiplication keeps 27 collections, one per size combination. Each
 * collection is one SharedPreferences key holding ';'-separated entries.
 */
final class WrongBook {

    private static final String PREFS = "detmatrix_wrong";

    /** Key of the single determinant collection. */
    static final String KEY_DET = "wrong_det";

    private WrongBook() {
    }

    static String keyForShape(int shapeIndex) {
        return "wrong_mm_" + shapeIndex;
    }

    /** One saved problem: A is m x k, B is k x n; B is empty in determinant mode. */
    static final class Item {
        final int m;
        final int k;
        final int n;
        final int[] a;
        final int[] b;
        boolean done;

        Item(int m, int k, int n, int[] a, int[] b, boolean done) {
            this.m = m;
            this.k = k;
            this.n = n;
            this.a = a;
            this.b = b;
            this.done = done;
        }

        String encode() {
            StringBuilder sb = new StringBuilder();
            sb.append(m).append(',').append(k).append(',').append(n).append('|');
            join(sb, a);
            sb.append('|');
            join(sb, b);
            sb.append('|').append(done ? 1 : 0);
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
                        "1".equals(parts[3].trim()));
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

    static void markDone(Context c, String key, int index) {
        List<Item> items = list(c, key);
        if (index < 0 || index >= items.size()) {
            return;
        }
        items.get(index).done = true;
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
