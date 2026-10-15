package com.example.detmatrix;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

/**
 * Mode two: matrix multiplication.
 *
 * The size is chosen by the {@link ShapeStepActivity} wizard and passed in:
 *   m = rows of A, k = columns of A = rows of B, n = columns of B
 * so A is m x k, B is k x n and the product C is m x n.
 */
public class MatMulActivity extends Activity {

    /** Random entries are drawn from [-10, 10]. */
    private static final int MIN_VALUE = -10;
    private static final int MAX_VALUE = 10;

    private static final String STATE_M = "m";
    private static final String STATE_K = "k";
    private static final String STATE_N = "n";
    private static final String STATE_A = "a";
    private static final String STATE_B = "b";
    private static final String STATE_HAS_NUMBERS = "hasNumbers";
    private static final String STATE_ANSWER_SHOWN = "answerShown";
    private static final String STATE_ELAPSED = "elapsed";
    private static final String STATE_RUNNING = "running";

    private final Random random = new Random();

    private int rowsA = 3;
    private int innerK = 3;
    private int colsB = 3;

    private LinearLayout rowAb;
    private LinearLayout matrixC;
    private TextView shapeLabel;
    private TextView timerView;
    private TextView answerHint;

    private int[][] valuesA;
    private int[][] valuesB;
    private TextView[][] cellsA;
    private TextView[][] cellsB;
    private TextView[][] cellsC;

    private boolean hasNumbers;
    private boolean resumeTimerOnStart;

    private final ElapsedTimer timer = new ElapsedTimer(new ElapsedTimer.Listener() {
        @Override
        public void onElapsed(long elapsedMillis) {
            if (timerView != null) {
                timerView.setText(getString(R.string.timer_format, elapsedMillis / 1000.0));
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matmul);

        rowsA = getIntent().getIntExtra(ShapeStepActivity.EXTRA_M, 3);
        innerK = getIntent().getIntExtra(ShapeStepActivity.EXTRA_K, 3);
        colsB = getIntent().getIntExtra(ShapeStepActivity.EXTRA_N, 3);

        timerView = (TextView) findViewById(R.id.timer);
        answerHint = (TextView) findViewById(R.id.answer_hint);
        shapeLabel = (TextView) findViewById(R.id.shape_label);
        rowAb = (LinearLayout) findViewById(R.id.row_ab);
        matrixC = (LinearLayout) findViewById(R.id.matrix_c);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ((Button) findViewById(R.id.next)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNext();
            }
        });
        ((Button) findViewById(R.id.show_answer)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShowAnswer();
            }
        });

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            rebuild();
        }
    }

    /** Rebuild the A, B and C grids for the current size and clear this round. */
    private void rebuild() {
        shapeLabel.setText(getString(R.string.shape_label, rowsA, innerK, innerK, colsB));

        int cell = cellSizePx();
        float textSp = Math.max(10f,
                cell / getResources().getDisplayMetrics().density * 0.34f);

        valuesA = new int[rowsA][innerK];
        valuesB = new int[innerK][colsB];
        cellsA = new TextView[rowsA][innerK];
        cellsB = new TextView[innerK][colsB];
        cellsC = new TextView[rowsA][colsB];

        rowAb.removeAllViews();
        matrixC.removeAllViews();

        LinearLayout boxA = new LinearLayout(this);
        boxA.setOrientation(LinearLayout.VERTICAL);
        buildGrid(boxA, cellsA, cell, textSp);

        TextView times = new TextView(this);
        times.setText("×");
        times.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(16f, textSp + 2f));
        times.setTypeface(null, Typeface.BOLD);
        times.setTextColor(getResources().getColor(R.color.title));
        times.setPadding(dp(7f), 0, dp(7f), 0);

        LinearLayout boxB = new LinearLayout(this);
        boxB.setOrientation(LinearLayout.VERTICAL);
        buildGrid(boxB, cellsB, cell, textSp);

        rowAb.addView(boxA);
        rowAb.addView(times);
        rowAb.addView(boxB);

        buildGrid(matrixC, cellsC, cell, textSp);

        hasNumbers = false;
        answerHint.setText("");
        timer.reset();
    }

    private void buildGrid(LinearLayout box, TextView[][] cells, int cellPx, float textSp) {
        int margin = Math.max(1, Math.round(cellPx * 0.05f));
        for (TextView[] rowCells : cells) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < rowCells.length; c++) {
                TextView tv = new TextView(this);
                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(cellPx, cellPx);
                lp.setMargins(margin, margin, margin, margin);
                tv.setLayoutParams(lp);
                tv.setBackgroundResource(R.drawable.cell_bg_small);
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(getResources().getColor(R.color.number));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
                tv.setTypeface(null, Typeface.BOLD);
                rowCells[c] = tv;
                row.addView(tv);
            }
            box.addView(row);
        }
    }

    /** A (innerK columns) and B (colsB columns) side by side; at most 4+4 = 8 columns. */
    private int cellSizePx() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float widthDp = dm.widthPixels / dm.density;
        // 10dp padding each side plus about 28dp for the "×".
        float available = widthDp - 20f - 28f;
        int totalCols = innerK + colsB;
        float cellDp = available / totalCols * 0.88f;
        if (cellDp > 54f) {
            cellDp = 54f;
        }
        if (cellDp < 26f) {
            cellDp = 26f;
        }
        return Math.round(cellDp * dm.density);
    }

    // --------------------------------------------------------------- actions

    /** "Next": draw new A and B, clear C, reset and start the timer. */
    private void onNext() {
        for (int r = 0; r < rowsA; r++) {
            for (int c = 0; c < innerK; c++) {
                int v = random.nextInt(MAX_VALUE - MIN_VALUE + 1) + MIN_VALUE;
                valuesA[r][c] = v;
                cellsA[r][c].setText(String.valueOf(v));
            }
        }
        for (int r = 0; r < innerK; r++) {
            for (int c = 0; c < colsB; c++) {
                int v = random.nextInt(MAX_VALUE - MIN_VALUE + 1) + MIN_VALUE;
                valuesB[r][c] = v;
                cellsB[r][c].setText(String.valueOf(v));
            }
        }
        for (int r = 0; r < rowsA; r++) {
            for (int c = 0; c < colsB; c++) {
                cellsC[r][c].setText("");
            }
        }
        hasNumbers = true;
        answerHint.setText("");
        timer.start();
    }

    /** "Show answer": stop the clock and fill in the whole product C. */
    private void onShowAnswer() {
        if (!hasNumbers) {
            answerHint.setText(getString(R.string.need_numbers_matmul));
            return;
        }
        timer.stop();
        showProduct();
        answerHint.setText("");
    }

    private void showProduct() {
        int[][] c = multiply(valuesA, valuesB);
        for (int r = 0; r < rowsA; r++) {
            for (int j = 0; j < colsB; j++) {
                cellsC[r][j].setText(String.valueOf(c[r][j]));
            }
        }
    }

    /** Matrix product: A(m x k) . B(k x n) = C(m x n). */
    static int[][] multiply(int[][] a, int[][] b) {
        int m = a.length;
        int k = a[0].length;
        int n = b[0].length;
        int[][] c = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int sum = 0;
                for (int t = 0; t < k; t++) {
                    sum += a[i][t] * b[t][j];
                }
                c[i][j] = sum;
            }
        }
        return c;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // --------------------------------------------------- lifecycle and state

    @Override
    protected void onPause() {
        super.onPause();
        resumeTimerOnStart = timer.isRunning();
        if (resumeTimerOnStart) {
            timer.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resumeTimerOnStart && hasNumbers) {
            resumeTimerOnStart = false;
            timer.startFrom(timer.elapsed());
        }
    }

    @Override
    protected void onDestroy() {
        timer.cancel();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_M, rowsA);
        outState.putInt(STATE_K, innerK);
        outState.putInt(STATE_N, colsB);
        outState.putBoolean(STATE_HAS_NUMBERS, hasNumbers);
        outState.putBoolean(STATE_ANSWER_SHOWN, hasNumbers && anyCellFilled(cellsC));
        outState.putLong(STATE_ELAPSED, timer.elapsed());
        outState.putBoolean(STATE_RUNNING, timer.isRunning());
        if (hasNumbers) {
            outState.putIntArray(STATE_A, flatten(valuesA));
            outState.putIntArray(STATE_B, flatten(valuesB));
        }
    }

    private void restoreState(Bundle inState) {
        rowsA = inState.getInt(STATE_M, rowsA);
        innerK = inState.getInt(STATE_K, innerK);
        colsB = inState.getInt(STATE_N, colsB);

        rebuild();

        hasNumbers = inState.getBoolean(STATE_HAS_NUMBERS, false);
        if (hasNumbers) {
            fillFromFlat(valuesA, cellsA, inState.getIntArray(STATE_A));
            fillFromFlat(valuesB, cellsB, inState.getIntArray(STATE_B));
            if (inState.getBoolean(STATE_ANSWER_SHOWN, false)) {
                showProduct();
            }
        }

        long elapsed = inState.getLong(STATE_ELAPSED, 0L);
        if (inState.getBoolean(STATE_RUNNING, false) && hasNumbers) {
            timer.startFrom(elapsed);
        } else {
            timer.setElapsed(elapsed);
        }
    }

    private static boolean anyCellFilled(TextView[][] cells) {
        if (cells == null) {
            return false;
        }
        for (TextView[] row : cells) {
            for (TextView tv : row) {
                if (tv != null && tv.getText().length() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int[] flatten(int[][] values) {
        int total = 0;
        for (int[] row : values) {
            total += row.length;
        }
        int[] flat = new int[total];
        int index = 0;
        for (int[] row : values) {
            for (int v : row) {
                flat[index++] = v;
            }
        }
        return flat;
    }

    private static void fillFromFlat(int[][] values, TextView[][] cells, int[] flat) {
        if (flat == null) {
            return;
        }
        int index = 0;
        for (int r = 0; r < values.length; r++) {
            for (int c = 0; c < values[r].length; c++) {
                if (index >= flat.length) {
                    return;
                }
                values[r][c] = flat[index];
                cells[r][c].setText(String.valueOf(flat[index]));
                index++;
            }
        }
    }
}
