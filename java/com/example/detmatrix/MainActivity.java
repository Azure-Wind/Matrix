package com.example.detmatrix;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

/** Determinant practice: a 3x3 matrix, a timer, and next / show-answer. */
public class MainActivity extends Activity {

    /** Matrix is 3x3. */
    private static final int SIZE = 3;

    /** Random entries are drawn from [-10, 10]. */
    private static final int MIN_VALUE = -10;
    private static final int MAX_VALUE = 10;

    /** Timer refresh interval. */
    private static final long TICK_MS = 50L;

    private static final int[][] CELL_IDS = {
            {R.id.cell00, R.id.cell01, R.id.cell02},
            {R.id.cell10, R.id.cell11, R.id.cell12},
            {R.id.cell20, R.id.cell21, R.id.cell22},
    };

    private static final String STATE_VALUES = "values";
    private static final String STATE_HAS_NUMBERS = "hasNumbers";
    private static final String STATE_ELAPSED = "elapsed";
    private static final String STATE_RUNNING = "running";
    private static final String STATE_ANSWER = "answer";

    private final TextView[][] cells = new TextView[SIZE][SIZE];
    private final int[][] values = new int[SIZE][SIZE];
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView timerView;
    private TextView answerView;
    private String answerText = "";

    /** System time the current run started at, minus the already elapsed part. */
    private long startTime;
    /** Accumulated time for the current run. */
    private long elapsedMillis;
    private boolean running;
    private boolean hasNumbers;
    /** Timer was paused because the app went to the background. */
    private boolean resumeTimerOnStart;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            elapsedMillis = System.currentTimeMillis() - startTime;
            updateTimerLabel();
            handler.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerView = (TextView) findViewById(R.id.timer);
        answerView = (TextView) findViewById(R.id.answer);

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c] = (TextView) findViewById(CELL_IDS[r][c]);
            }
        }

        Button nextButton = (Button) findViewById(R.id.next);
        Button showAnswerButton = (Button) findViewById(R.id.show_answer);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNext();
            }
        });

        showAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShowAnswer();
            }
        });

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            updateTimerLabel();
            setAnswer(getString(R.string.answer_placeholder));
        }
    }

    /** "Next": clear the old numbers, draw new ones, reset and start the timer. */
    private void onNext() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                // Clear the previous round before writing the new number.
                cells[r][c].setText("");
                int v = random.nextInt(MAX_VALUE - MIN_VALUE + 1) + MIN_VALUE;
                values[r][c] = v;
                cells[r][c].setText(String.valueOf(v));
            }
        }
        hasNumbers = true;
        setAnswer("");
        startTimer(0L);
    }

    /** "Show answer": stop the clock and show the determinant. */
    private void onShowAnswer() {
        if (!hasNumbers) {
            setAnswer(getString(R.string.need_numbers));
            return;
        }
        pauseTimer();
        setAnswer(getString(R.string.answer_format, determinant(values)));
    }

    /** 3x3 determinant: a(ei-fh) - b(di-fg) + c(dh-eg), exact integer arithmetic. */
    static int determinant(int[][] m) {
        int a = m[0][0], b = m[0][1], c = m[0][2];
        int d = m[1][0], e = m[1][1], f = m[1][2];
        int g = m[2][0], h = m[2][1], i = m[2][2];
        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
    }

    private void startTimer(long alreadyElapsedMillis) {
        handler.removeCallbacks(ticker);
        elapsedMillis = alreadyElapsedMillis;
        startTime = System.currentTimeMillis() - alreadyElapsedMillis;
        running = true;
        updateTimerLabel();
        handler.post(ticker);
    }

    private void pauseTimer() {
        handler.removeCallbacks(ticker);
        if (running) {
            elapsedMillis = System.currentTimeMillis() - startTime;
            running = false;
        }
        updateTimerLabel();
    }

    private void updateTimerLabel() {
        if (timerView == null) {
            return;
        }
        timerView.setText(getString(R.string.timer_format, elapsedMillis / 1000.0));
    }

    private void setAnswer(String text) {
        answerText = text;
        if (answerView != null) {
            answerView.setText(text);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause while in the background so time away is not counted.
        resumeTimerOnStart = running;
        if (running) {
            pauseTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resumeTimerOnStart && hasNumbers) {
            resumeTimerOnStart = false;
            startTimer(elapsedMillis);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (hasNumbers) {
            int[] flat = new int[SIZE * SIZE];
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    flat[r * SIZE + c] = values[r][c];
                }
            }
            outState.putIntArray(STATE_VALUES, flat);
        }
        outState.putBoolean(STATE_HAS_NUMBERS, hasNumbers);
        outState.putLong(STATE_ELAPSED, running ? System.currentTimeMillis() - startTime : elapsedMillis);
        outState.putBoolean(STATE_RUNNING, running);
        outState.putString(STATE_ANSWER, answerText);
    }

    private void restoreState(Bundle inState) {
        hasNumbers = inState.getBoolean(STATE_HAS_NUMBERS, false);
        int[] flat = inState.getIntArray(STATE_VALUES);
        if (hasNumbers && flat != null && flat.length == SIZE * SIZE) {
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    values[r][c] = flat[r * SIZE + c];
                    cells[r][c].setText(String.valueOf(values[r][c]));
                }
            }
        }
        setAnswer(inState.getString(STATE_ANSWER, ""));
        elapsedMillis = inState.getLong(STATE_ELAPSED, 0L);
        running = false;
        updateTimerLabel();
        if (inState.getBoolean(STATE_RUNNING, false) && hasNumbers) {
            startTimer(elapsedMillis);
        }
    }
}
