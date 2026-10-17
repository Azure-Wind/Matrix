package com.example.detmatrix;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

/**
 * Mode two: matrix multiplication. Also used as the timed redo screen when a
 * problem is opened from the wrong-answer book.
 *
 * The size is chosen by the {@link ShapeStepActivity} wizard and passed in:
 *   m = rows of A, k = columns of A = rows of B, n = columns of B
 * so A is m x k, B is k x n and the product C is m x n.
 *
 * "Show answer" reveals exactly one cell of C per tap, row by row from left to
 * right. The clock is stopped on the first tap, and the time is recorded once
 * the last cell is revealed (in redo mode the problem is then marked done).
 * The result cells are deliberately larger than the A/B cells so four-character
 * values such as -350 fit.
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
    private static final String STATE_REVEALED = "revealed";
    private static final String STATE_RECORDED = "recorded";
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
    private TextView practiceNote;

    private int[][] valuesA;
    private int[][] valuesB;
    private int[][] product;
    private TextView[][] cellsA;
    private TextView[][] cellsB;
    private TextView[][] cellsC;

    private boolean hasNumbers;
    private boolean resumeTimerOnStart;
    /** How many result cells have been revealed so far. */
    private int revealCount;
    /** This round's time has already been recorded. */
    private boolean roundRecorded;
    /** This round has already been added to the wrong-answer book. */
    private boolean markedWrong;

    private boolean practiceMode;
    private String wrongKey;
    private int wrongIndex;
    /** In redo mode: this visit has already been reported as wrong. */
    private boolean practiceMarkedWrong;

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

        practiceMode = getIntent().getBooleanExtra(Practice.EXTRA_PRACTICE, false);
        wrongKey = getIntent().getStringExtra(Practice.EXTRA_WRONG_KEY);
        wrongIndex = getIntent().getIntExtra(Practice.EXTRA_WRONG_INDEX, -1);

        rowsA = getIntent().getIntExtra(ShapeStepActivity.EXTRA_M, 3);
        innerK = getIntent().getIntExtra(ShapeStepActivity.EXTRA_K, 3);
        colsB = getIntent().getIntExtra(ShapeStepActivity.EXTRA_N, 3);

        timerView = (TextView) findViewById(R.id.timer);
        answerHint = (TextView) findViewById(R.id.answer_hint);
        practiceNote = (TextView) findViewById(R.id.practice_note);
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
        ((Button) findViewById(R.id.mark_wrong)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMarkWrong();
            }
        });
        findViewById(R.id.wrong_book).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MatMulActivity.this, WrongShapesActivity.class));
            }
        });
        findViewById(R.id.stats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MatMulActivity.this, StatsActivity.class)
                        .putExtra(StatsActivity.EXTRA_MODE, StatsStore.MODE_MATMUL));
            }
        });

        if (practiceMode) {
            applyPracticeUi();
        }

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else if (practiceMode) {
            rebuild();
            loadPracticeValues();
        } else {
            rebuild();
        }
    }

    /**
     * Redo mode: change the title and hide the controls that make no sense here.
     * "Wrong" stays visible so a problem can be reported wrong again.
     */
    private void applyPracticeUi() {
        ((TextView) findViewById(R.id.screen_title)).setText(R.string.practice_hint);
        findViewById(R.id.next).setVisibility(View.GONE);
        findViewById(R.id.wrong_book).setVisibility(View.GONE);
        findViewById(R.id.stats).setVisibility(View.GONE);
    }

    /** Redo mode: fill A and B with the saved problem and start the clock. */
    private void loadPracticeValues() {
        int[] flatA = getIntent().getIntArrayExtra(Practice.EXTRA_VALUES_A);
        int[] flatB = getIntent().getIntArrayExtra(Practice.EXTRA_VALUES_B);
        if (flatA == null || flatB == null) {
            finish();
            return;
        }
        fillFromFlat(valuesA, cellsA, flatA);
        fillFromFlat(valuesB, cellsB, flatB);
        hasNumbers = true;
        timer.start();
    }

    /** Rebuild the A, B and C grids for the current size and clear this round. */
    private void rebuild() {
        shapeLabel.setText(getString(R.string.shape_label, rowsA, innerK, innerK, colsB));

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int cell = cellSizePx();
        float textSp = Math.max(10f, cell / dm.density * 0.34f);

        // Result cells are sized separately so -350 style values fit.
        int cellC = cellSizeCPx();
        float textSpC = Math.max(12f, cellC / dm.density * 0.34f);

        valuesA = new int[rowsA][innerK];
        valuesB = new int[innerK][colsB];
        cellsA = new TextView[rowsA][innerK];
        cellsB = new TextView[innerK][colsB];
        cellsC = new TextView[rowsA][colsB];
        product = null;
        revealCount = 0;
        roundRecorded = false;
        markedWrong = false;

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

        buildGrid(matrixC, cellsC, cellC, textSpC);

        hasNumbers = false;
        answerHint.setText("");
        practiceNote.setVisibility(View.GONE);
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

    /** C sits on its own row with at most 4 columns, so it can afford larger cells. */
    private int cellSizeCPx() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float widthDp = dm.widthPixels / dm.density;
        float available = widthDp - 20f;
        float cellDp = available / colsB * 0.9f;
        if (cellDp > 58f) {
            cellDp = 58f;
        }
        if (cellDp < 42f) {
            cellDp = 42f;
        }
        return Math.round(cellDp * dm.density);
    }

    private int totalResults() {
        return rowsA * colsB;
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
        product = null;
        revealCount = 0;
        roundRecorded = false;
        markedWrong = false;
        hasNumbers = true;
        answerHint.setText("");
        practiceNote.setVisibility(View.GONE);
        timer.start();
    }

    /** "Show answer": reveal the next cell of C; the first tap stops the clock. */
    private void onShowAnswer() {
        if (!hasNumbers) {
            answerHint.setText(getString(R.string.need_numbers_matmul));
            return;
        }
        if (revealCount == 0) {
            timer.stop();
        }
        if (revealCount >= totalResults()) {
            return;
        }
        if (product == null) {
            product = multiply(valuesA, valuesB);
        }
        int r = revealCount / colsB;
        int c = revealCount % colsB;
        cellsC[r][c].setText(String.valueOf(product[r][c]));
        revealCount++;

        if (revealCount >= totalResults()) {
            onAllRevealed();
        } else {
            answerHint.setText(getString(R.string.reveal_progress, revealCount, totalResults()));
        }
    }

    /**
     * The last cell is revealed: in normal mode record the time, in redo mode
     * count this redo as correct.
     */
    private void onAllRevealed() {
        answerHint.setText("");
        if (practiceMode) {
            // Counts as correct unless the user already said they got it wrong.
            if (!practiceMarkedWrong && wrongKey != null) {
                int streak = WrongBook.markCorrect(this, wrongKey, wrongIndex);
                practiceNote.setText(streak >= WrongBook.MASTERY_STREAK
                        ? R.string.practice_done : R.string.practice_done_again);
                practiceNote.setVisibility(View.VISIBLE);
            }
        } else if (!roundRecorded) {
            roundRecorded = true;
            StatsStore.record(this, StatsStore.MODE_MATMUL, timer.elapsed());
        }
    }

    /**
     * "Wrong". Normal mode: add the current problem to the collection for this
     * size. Redo mode: report that this redo went wrong, which turns it red.
     */
    private void onMarkWrong() {
        if (!hasNumbers) {
            toast(R.string.toast_need_numbers);
            return;
        }
        if (practiceMode) {
            if (practiceMarkedWrong) {
                toast(R.string.toast_redo_wrong_once);
                return;
            }
            if (wrongKey != null) {
                WrongBook.markWrongAttempt(this, wrongKey, wrongIndex);
            }
            practiceMarkedWrong = true;
            practiceNote.setVisibility(View.GONE);
            toast(R.string.toast_redo_wrong);
            return;
        }
        if (markedWrong) {
            toast(R.string.toast_already);
            return;
        }
        String key = WrongBook.keyForShape(Shapes.indexOf(rowsA, innerK, colsB));
        boolean added = WrongBook.add(this, key, new WrongBook.Item(
                rowsA, innerK, colsB, flatten(valuesA), flatten(valuesB), 0, false));
        markedWrong = true;
        toast(added ? R.string.toast_marked : R.string.toast_already);
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
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
        outState.putInt(STATE_REVEALED, revealCount);
        outState.putBoolean(STATE_RECORDED, roundRecorded);
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
            int revealed = inState.getInt(STATE_REVEALED, 0);
            if (revealed > 0) {
                product = multiply(valuesA, valuesB);
                for (int i = 0; i < revealed && i < totalResults(); i++) {
                    int r = i / colsB;
                    int c = i % colsB;
                    cellsC[r][c].setText(String.valueOf(product[r][c]));
                }
                revealCount = Math.min(revealed, totalResults());
                if (revealCount < totalResults()) {
                    answerHint.setText(getString(R.string.reveal_progress,
                            revealCount, totalResults()));
                }
            }
        }
        roundRecorded = inState.getBoolean(STATE_RECORDED, false);

        long elapsed = inState.getLong(STATE_ELAPSED, 0L);
        if (inState.getBoolean(STATE_RUNNING, false) && hasNumbers) {
            timer.startFrom(elapsed);
        } else {
            timer.setElapsed(elapsed);
        }
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
