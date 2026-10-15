import java.lang.reflect.Method;
import java.util.Random;

/**
 * Verifies the determinant routine that is actually compiled into the APK
 * (com.example.detmatrix.MainActivity#determinant) against an independent
 * implementation: the full 6-term permutation (Leibniz) expansion.
 */
public class DetTest {

    static int failures = 0;

    /** Independent reference: sum over all 3! permutations with parity sign. */
    static int referenceDet(int[][] m) {
        int[][] perms = {
                {0, 1, 2}, {0, 2, 1}, {1, 0, 2},
                {1, 2, 0}, {2, 0, 1}, {2, 1, 0}
        };
        int sum = 0;
        for (int[] q : perms) {
            int prod = m[0][q[0]] * m[1][q[1]] * m[2][q[2]];
            int inversions = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = i + 1; j < 3; j++) {
                    if (q[i] > q[j]) inversions++;
                }
            }
            sum += ((inversions % 2 == 0) ? 1 : -1) * prod;
        }
        return sum;
    }

    static void check(boolean ok, String message) {
        if (!ok) {
            failures++;
            System.out.println("  FAIL " + message);
        }
    }

    public static void main(String[] args) throws Exception {
        Class<?> cls = Class.forName("com.example.detmatrix.MainActivity");
        Method det = cls.getDeclaredMethod("determinant", int[][].class);
        det.setAccessible(true);

        // ---- known matrices ---------------------------------------------
        int[][][] known = {
                {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}},          // 0
                {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}},          // 1
                {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}},          // 0 (singular)
                {{2, 0, 0}, {0, 3, 0}, {0, 0, 4}},          // 24
                {{6, 1, 1}, {4, -2, 5}, {2, 8, 7}},         // -306
                {{-10, 10, -10}, {10, -10, 10}, {-10, 10, -10}}, // 0
        };
        int[] expected = {0, 1, 0, 24, -306, 0};
        for (int i = 0; i < known.length; i++) {
            int got = (Integer) det.invoke(null, (Object) known[i]);
            check(got == expected[i], "known[" + i + "] expected=" + expected[i] + " got=" + got);
        }
        System.out.println("known matrices checked: " + known.length);

        // ---- exhaustive over every entry in [-2, 2]  (5^9 = 1,953,125) ----
        int[] v = new int[9];
        int[][] m = new int[3][3];
        long combos = 0;
        for (v[0] = -2; v[0] <= 2; v[0]++)
        for (v[1] = -2; v[1] <= 2; v[1]++)
        for (v[2] = -2; v[2] <= 2; v[2]++)
        for (v[3] = -2; v[3] <= 2; v[3]++)
        for (v[4] = -2; v[4] <= 2; v[4]++)
        for (v[5] = -2; v[5] <= 2; v[5]++)
        for (v[6] = -2; v[6] <= 2; v[6]++)
        for (v[7] = -2; v[7] <= 2; v[7]++)
        for (v[8] = -2; v[8] <= 2; v[8]++) {
            for (int k = 0; k < 9; k++) m[k / 3][k % 3] = v[k];
            int got = (Integer) det.invoke(null, (Object) m);
            combos++;
            if (got != referenceDet(m)) {
                failures++;
                if (failures < 10) System.out.println("  FAIL exhaustive");
            }
        }
        System.out.println("exhaustive [-2..2] matrices: " + combos);

        // ---- random full-range [-10, 10] matrices ------------------------
        Random rnd = new Random(12345);
        int minSeen = Integer.MAX_VALUE, maxSeen = Integer.MIN_VALUE;
        int maxAbsDet = 0;
        for (int t = 0; t < 300000; t++) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int val = rnd.nextInt(21) - 10;   // same expression as the app
                    m[r][c] = val;
                    minSeen = Math.min(minSeen, val);
                    maxSeen = Math.max(maxSeen, val);
                }
            }
            int got = (Integer) det.invoke(null, (Object) m);
            maxAbsDet = Math.max(maxAbsDet, Math.abs(got));
            if (got != referenceDet(m)) {
                failures++;
                if (failures < 10) System.out.println("  FAIL random");
            }
        }
        System.out.println("random matrices: 300000");
        System.out.println("app value range produced: [" + minSeen + ", " + maxSeen + "]");
        System.out.println("max |det| observed: " + maxAbsDet);

        System.out.println();
        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURES");
        if (failures != 0) System.exit(1);
    }
}
