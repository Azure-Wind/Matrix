package com.example.detmatrix;

/**
 * Size space for matrix multiplication:
 *   m in {2,3,4} = rows of A
 *   k in {2,3,4} = columns of A = rows of B
 *   n in {2,3,4} = columns of B
 * giving 3 x 3 x 3 = 27 combinations.
 */
final class Shapes {

    static final int[] OPTIONS = {2, 3, 4};
    static final int COUNT = 27;

    /** index -> {m, k, n}; ordered by k, then m, then n. */
    static final int[][] ALL = buildAll();

    private Shapes() {
    }

    private static int[][] buildAll() {
        int[][] out = new int[COUNT][];
        int i = 0;
        for (int k : OPTIONS) {
            for (int m : OPTIONS) {
                for (int n : OPTIONS) {
                    out[i++] = new int[]{m, k, n};
                }
            }
        }
        return out;
    }

    private static int pos(int value) {
        for (int i = 0; i < OPTIONS.length; i++) {
            if (OPTIONS[i] == value) {
                return i;
            }
        }
        return 0;
    }

    static int indexOf(int m, int k, int n) {
        return (pos(k) * OPTIONS.length + pos(m)) * OPTIONS.length + pos(n);
    }

    /** e.g. "(3x2,2x4)" meaning A is 3x2 and B is 2x4 (x is U+00D7). */
    static String label(int index) {
        int[] s = ALL[index];
        return "(" + s[0] + "\u00D7" + s[1] + "," + s[1] + "\u00D7" + s[2] + ")";
    }
}
