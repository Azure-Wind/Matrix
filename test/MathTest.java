import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Verifies the maths routines that are actually compiled into the APK:
 *   com.example.detmatrix.DetActivity#determinant    (3x3 determinant)
 *   com.example.detmatrix.MatMulActivity#multiply    (matrix product)
 *
 * The product is cross-checked against an independent formulation, and the
 * selectable shape space is pinned against an explicit list so an accidental
 * change to the wizard's option arrays is caught.
 */
public class MathTest {

    static int failures = 0;

    // ------------------------------------------------------------ references

    /** Independent determinant: full 3! permutation (Leibniz) expansion. */
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

    /**
     * Independent product: accumulate rank-1 outer products.
     * Different loop order / formulation from the shipped dot-product version.
     */
    static int[][] referenceMultiply(int[][] a, int[][] b) {
        int m = a.length, k = a[0].length, n = b[0].length;
        int[][] c = new int[m][n];
        for (int t = 0; t < k; t++) {
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    c[i][j] += a[i][t] * b[t][j];
                }
            }
        }
        return c;
    }

    static int[] staticIntArray(Class<?> cls, String name) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return (int[]) f.get(null);
    }

    static void check(boolean ok, String message) {
        if (!ok) {
            failures++;
            System.out.println("  FAIL " + message);
        }
    }

    // ---------------------------------------------------------------- tests

    static void testDeterminant() throws Exception {
        System.out.println("--- 3x3 determinant ---");
        Class<?> cls = Class.forName("com.example.detmatrix.DetActivity");
        Method det = cls.getDeclaredMethod("determinant", int[][].class);
        det.setAccessible(true);

        int[][][] known = {
                {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}},
                {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}},
                {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}},
                {{2, 0, 0}, {0, 3, 0}, {0, 0, 4}},
                {{6, 1, 1}, {4, -2, 5}, {2, 8, 7}},
                {{-10, 10, -10}, {10, -10, 10}, {-10, 10, -10}},
        };
        int[] expected = {0, 1, 0, 24, -306, 0};
        for (int i = 0; i < known.length; i++) {
            int got = (Integer) det.invoke(null, (Object) known[i]);
            check(got == expected[i], "known[" + i + "] expected=" + expected[i] + " got=" + got);
        }
        System.out.println("  known matrices: " + known.length + " checked");

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
            for (int t = 0; t < 9; t++) m[t / 3][t % 3] = v[t];
            int got = (Integer) det.invoke(null, (Object) m);
            combos++;
            if (got != referenceDet(m)) {
                failures++;
                if (failures < 6) System.out.println("  FAIL exhaustive");
            }
        }
        System.out.println("  exhaustive [-2..2]^9: " + combos);

        Random rnd = new Random(12345);
        int maxAbs = 0;
        for (int t = 0; t < 300000; t++) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    m[r][c] = rnd.nextInt(21) - 10;
                }
            }
            int got = (Integer) det.invoke(null, (Object) m);
            maxAbs = Math.max(maxAbs, Math.abs(got));
            if (got != referenceDet(m)) {
                failures++;
                if (failures < 6) System.out.println("  FAIL random");
            }
        }
        System.out.println("  random full-range: 300000   max |det| = " + maxAbs);
    }

    static void testMultiply() throws Exception {
        System.out.println("--- matrix product ---");
        Class<?> cls = Class.forName("com.example.detmatrix.MatMulActivity");
        Method mul = cls.getDeclaredMethod("multiply", int[][].class, int[][].class);
        mul.setAccessible(true);

        Object[][] cases = {
                {new int[][]{{1, 2}, {3, 4}}, new int[][]{{5, 6}, {7, 8}},
                        new int[][]{{19, 22}, {43, 50}}},
                {new int[][]{{1, 2, 3}, {4, 5, 6}}, new int[][]{{7, 8}, {9, 10}, {11, 12}},
                        new int[][]{{58, 64}, {139, 154}}},
                {new int[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}, new int[][]{{2, -3}, {4, 5}, {-6, 7}},
                        new int[][]{{2, -3}, {4, 5}, {-6, 7}}},
                {new int[][]{{0, 0}, {0, 0}}, new int[][]{{9, 8}, {7, 6}},
                        new int[][]{{0, 0}, {0, 0}}},
                {new int[][]{{-1, 2}, {3, -4}}, new int[][]{{-5, -6}, {-7, -8}},
                        new int[][]{{-9, -10}, {13, 14}}},
                // 4x3 * 3x4 -> 4x4 (only reachable now that k=3 allows size 4)
                {new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}, {10, 11, 12}},
                        new int[][]{{1, 0, 0, 1}, {0, 1, 0, 1}, {0, 0, 1, 1}},
                        new int[][]{{1, 2, 3, 6}, {4, 5, 6, 15}, {7, 8, 9, 24}, {10, 11, 12, 33}}},
        };
        for (int ci = 0; ci < cases.length; ci++) {
            int[][] got = (int[][]) mul.invoke(null, cases[ci][0], cases[ci][1]);
            check(Arrays.deepEquals(got, (int[][]) cases[ci][2]),
                    "known[" + ci + "] got=" + Arrays.deepToString(got)
                            + " want=" + Arrays.deepToString((int[][]) cases[ci][2]));
        }
        System.out.println("  known products: " + cases.length + " checked");

        // ---- shape space, read from the wizard's own option arrays ----
        Class<?> stepCls = Class.forName("com.example.detmatrix.ShapeStepActivity");
        int[] sizes = staticIntArray(stepCls, "OPTIONS_SIZE");
        int[] inners = staticIntArray(stepCls, "OPTIONS_INNER");
        System.out.println("  wizard options: size=" + Arrays.toString(sizes)
                + " inner=" + Arrays.toString(inners));

        Set<String> produced = new TreeSet<String>();
        for (int k : inners) {
            for (int m : sizes) {
                for (int n : sizes) {
                    produced.add(m + "x" + k + "/" + k + "x" + n);
                }
            }
        }
        // 3 x 3 x 3 = 27 combinations
        Set<String> required = new TreeSet<String>(Arrays.asList(
                "2x2/2x2", "2x2/2x3", "2x2/2x4",
                "3x2/2x2", "3x2/2x3", "3x2/2x4",
                "4x2/2x2", "4x2/2x3", "4x2/2x4",
                "2x3/3x2", "2x3/3x3", "2x3/3x4",
                "3x3/3x2", "3x3/3x3", "3x3/3x4",
                "4x3/3x2", "4x3/3x3", "4x3/3x4",
                "2x4/4x2", "2x4/4x3", "2x4/4x4",
                "3x4/4x2", "3x4/4x3", "3x4/4x4",
                "4x4/4x2", "4x4/4x3", "4x4/4x4"));
        System.out.println("  selectable shapes: " + produced.size() + " (expected 27)");
        if (!produced.equals(required)) {
            Set<String> missing = new TreeSet<String>(required);
            missing.removeAll(produced);
            Set<String> extra = new TreeSet<String>(produced);
            extra.removeAll(required);
            check(false, "shape space  missing=" + missing + " extra=" + extra);
        }

        // ---- the Shapes index table must line up with the wizard's options ----
        Class<?> shapesCls = Class.forName("com.example.detmatrix.Shapes");
        Field countField = shapesCls.getDeclaredField("COUNT");
        countField.setAccessible(true);
        int shapeCount = countField.getInt(null);
        Field allField = shapesCls.getDeclaredField("ALL");
        allField.setAccessible(true);
        int[][] all = (int[][]) allField.get(null);
        Method indexOf = shapesCls.getDeclaredMethod("indexOf", int.class, int.class, int.class);
        indexOf.setAccessible(true);
        Method label = shapesCls.getDeclaredMethod("label", int.class);
        label.setAccessible(true);

        check(shapeCount == 27, "Shapes.COUNT = " + shapeCount);
        check(all.length == 27, "Shapes.ALL length = " + all.length);

        Set<String> covered = new TreeSet<String>();
        for (int k : inners) {
            for (int m : sizes) {
                for (int n : sizes) {
                    int idx = (Integer) indexOf.invoke(null, m, k, n);
                    if (idx < 0 || idx >= all.length) {
                        check(false, "indexOf(" + m + "," + k + "," + n + ") = " + idx);
                        continue;
                    }
                    int[] s = all[idx];
                    if (s[0] != m || s[1] != k || s[2] != n) {
                        check(false, "Shapes.ALL[" + idx + "] = " + Arrays.toString(s)
                                + " expected {" + m + "," + k + "," + n + "}");
                    }
                    covered.add(idx + ":" + label.invoke(null, idx));
                }
            }
        }
        check(covered.size() == 27, "Shapes indices covered = " + covered.size());
        System.out.println("  Shapes index table: " + covered.size()
                + " indices, 0..26 all map back to their own (m,k,n)");
        System.out.println("  sample labels: " + label.invoke(null, 0) + "  "
                + label.invoke(null, 13) + "  " + label.invoke(null, 26));

        // the 13 shapes originally requested must all still be reachable
        Set<String> original13 = new TreeSet<String>(Arrays.asList(
                "2x2/2x2", "2x2/2x3", "2x2/2x4",
                "3x2/2x2", "3x2/2x3", "3x2/2x4",
                "4x2/2x2", "4x2/2x3", "4x2/2x4",
                "2x3/3x2", "2x3/3x3",
                "3x3/3x2", "3x3/3x3"));
        check(produced.containsAll(original13), "original 13 shapes no longer all reachable");

        // ---- every shape: random full-range comparison ----
        Random rnd = new Random(999);
        int perShape = 4000;
        long checks = 0;
        int maxAbs = 0;
        int widestCols = 0;
        for (int k : inners) {
            for (int m : sizes) {
                for (int n : sizes) {
                    widestCols = Math.max(widestCols, k + n);
                    for (int t = 0; t < perShape; t++) {
                        int[][] a = new int[m][k];
                        int[][] b = new int[k][n];
                        for (int i = 0; i < m; i++)
                            for (int j = 0; j < k; j++) a[i][j] = rnd.nextInt(21) - 10;
                        for (int i = 0; i < k; i++)
                            for (int j = 0; j < n; j++) b[i][j] = rnd.nextInt(21) - 10;
                        int[][] got = (int[][]) mul.invoke(null, a, b);
                        int[][] want = referenceMultiply(a, b);
                        checks++;
                        for (int i = 0; i < m; i++)
                            for (int j = 0; j < n; j++)
                                maxAbs = Math.max(maxAbs, Math.abs(got[i][j]));
                        if (!Arrays.deepEquals(got, want)) {
                            failures++;
                            if (failures < 6) {
                                System.out.println("  FAIL shape " + m + "x" + k + "/" + k + "x" + n);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("  random checks over all shapes: " + checks + "   max |c| = " + maxAbs);
        System.out.println("  widest A+B row to lay out: " + widestCols + " cell columns");

        // ---- exhaustive 2x2 * 2x2 over [-2,2]^8 ----
        long combos = 0;
        int[] v = new int[8];
        for (v[0] = -2; v[0] <= 2; v[0]++)
        for (v[1] = -2; v[1] <= 2; v[1]++)
        for (v[2] = -2; v[2] <= 2; v[2]++)
        for (v[3] = -2; v[3] <= 2; v[3]++)
        for (v[4] = -2; v[4] <= 2; v[4]++)
        for (v[5] = -2; v[5] <= 2; v[5]++)
        for (v[6] = -2; v[6] <= 2; v[6]++)
        for (v[7] = -2; v[7] <= 2; v[7]++) {
            int[][] a = {{v[0], v[1]}, {v[2], v[3]}};
            int[][] b = {{v[4], v[5]}, {v[6], v[7]}};
            int[][] got = (int[][]) mul.invoke(null, a, b);
            combos++;
            if (!Arrays.deepEquals(got, referenceMultiply(a, b))) {
                failures++;
                if (failures < 6) System.out.println("  FAIL exhaustive 2x2");
            }
        }
        System.out.println("  exhaustive 2x2*2x2 over [-2..2]^8: " + combos);
    }

    /**
     * Wrong-book colour rules, checked through the real (pure) WrongBook.state:
     * two correct redos in a row make an entry green; a wrong redo turns it red
     * at once, even from green; one correct redo after red gives white again.
     */
    static void testWrongBookState() throws Exception {
        System.out.println("--- wrong-book state rules ---");
        Class<?> cls = Class.forName("com.example.detmatrix.WrongBook");
        Method state = cls.getDeclaredMethod("state", int.class, boolean.class);
        state.setAccessible(true);
        Field fWhite = cls.getDeclaredField("STATE_WHITE");
        Field fRed = cls.getDeclaredField("STATE_RED");
        Field fGreen = cls.getDeclaredField("STATE_GREEN");
        setAccessible(fWhite);
        setAccessible(fRed);
        setAccessible(fGreen);
        int white = fWhite.getInt(null);
        int red = fRed.getInt(null);
        int green = fGreen.getInt(null);
        check(white != red && red != green && white != green, "states must be distinct");

        // (streak, lastWrong, expected)
        int[][] cases = {
                {0, 0, white},
                {1, 0, white},
                {2, 0, green},
                {3, 0, green},
                {0, 1, red},
                {1, 1, red},
        };
        for (int[] c : cases) {
            int got = (Integer) state.invoke(null, c[0], c[1] == 1);
            check(got == c[2], "state(" + c[0] + "," + (c[1] == 1) + ") = " + got
                    + " expected " + c[2]);
        }

        // Walk the sequence the user described and check the colour each time.
        int streak = 0;
        boolean lastWrong = false;
        String[] steps = {"correct", "correct", "wrong", "correct", "correct", "wrong"};
        int[] want = {white, green, red, white, green, red};
        for (int i = 0; i < steps.length; i++) {
            if (steps[i].equals("correct")) {
                streak = Math.min(2, streak + 1);
                lastWrong = false;
            } else {
                streak = 0;
                lastWrong = true;
            }
            int got = (Integer) state.invoke(null, streak, lastWrong);
            check(got == want[i], "after " + steps[i] + " #" + (i + 1)
                    + " state = " + got + " expected " + want[i]);
        }
        System.out.println("  sequence correct,correct,wrong,correct,correct,wrong");
        System.out.println("  colours white,green,red,white,green,red  all as expected");
    }

    static void setAccessible(Field f) {
        f.setAccessible(true);
    }

    public static void main(String[] args) throws Exception {
        testDeterminant();
        System.out.println();
        testMultiply();
        System.out.println();
        testWrongBookState();
        System.out.println();
        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURES");
        if (failures != 0) System.exit(1);
    }
}
