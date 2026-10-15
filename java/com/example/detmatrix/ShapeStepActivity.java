package com.example.detmatrix;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Size-selection wizard for matrix multiplication. The same Activity is reused
 * three times:
 *   step 1: pick m, the number of rows of A
 *   step 2: pick k, the number of columns of A = rows of B
 *   step 3: pick n, the number of columns of B
 *
 * The back button simply calls finish(), which naturally returns to the previous
 * step -- that instance is still on the back stack with its selection intact.
 */
public class ShapeStepActivity extends Activity {

    static final String EXTRA_STEP = "step";
    static final String EXTRA_M = "m";
    static final String EXTRA_K = "k";
    static final String EXTRA_N = "n";

    /** Allowed values for rows of A / columns of B. */
    static final int[] OPTIONS_SIZE = {2, 3, 4};
    /** Allowed values for columns of A = rows of B. */
    static final int[] OPTIONS_INNER = {2, 3, 4};

    private static final String STATE_SELECTED = "selected";

    private int step;
    private int chosenM;
    private int chosenK;

    private int[] options;
    private int selected;

    private LinearLayout chipRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shape_step);

        step = getIntent().getIntExtra(EXTRA_STEP, 1);
        chosenM = getIntent().getIntExtra(EXTRA_M, 0);
        chosenK = getIntent().getIntExtra(EXTRA_K, 0);

        options = (step == 2) ? OPTIONS_INNER : OPTIONS_SIZE;
        selected = options[0];

        chipRow = (LinearLayout) findViewById(R.id.chip_row);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ((Button) findViewById(R.id.confirm)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConfirm();
            }
        });

        applyStepTexts();
        buildChips();

        if (savedInstanceState != null) {
            int restored = savedInstanceState.getInt(STATE_SELECTED, options[0]);
            if (contains(options, restored)) {
                selected = restored;
                buildChips();
            }
        }
    }

    private void applyStepTexts() {
        ((TextView) findViewById(R.id.step_indicator))
                .setText(getString(R.string.step_indicator, step));

        int titleRes;
        int hintRes;
        if (step == 1) {
            titleRes = R.string.step1_title;
            hintRes = R.string.step1_hint;
        } else if (step == 2) {
            titleRes = R.string.step2_title;
            hintRes = R.string.step2_hint;
        } else {
            titleRes = R.string.step3_title;
            hintRes = R.string.step3_hint;
        }
        ((TextView) findViewById(R.id.step_title)).setText(titleRes);
        ((TextView) findViewById(R.id.step_hint)).setText(hintRes);

        // Summary of what has been chosen so far.
        String chosenText;
        if (step == 1) {
            chosenText = "";
        } else if (step == 2) {
            chosenText = getString(R.string.chosen_m, chosenM);
        } else {
            chosenText = getString(R.string.chosen_mk, chosenM, chosenK);
        }
        ((TextView) findViewById(R.id.chosen)).setText(chosenText);
    }

    private void buildChips() {
        chipRow.removeAllViews();
        for (final int value : options) {
            boolean isSelected = (value == selected);

            TextView chip = new TextView(this);
            chip.setText(String.valueOf(value));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setBackgroundResource(isSelected
                    ? R.drawable.chip_bg_selected : R.drawable.chip_bg);
            chip.setTextColor(getResources().getColor(isSelected
                    ? R.color.chip_text_selected : R.color.chip_text));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(62f), dp(52f));
            lp.setMargins(dp(6f), 0, dp(6f), 0);
            chip.setLayoutParams(lp);

            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selected = value;
                    buildChips();
                }
            });
            chipRow.addView(chip);
        }
    }

    /** Carry the chosen values forward; the last step opens the working screen. */
    private void onConfirm() {
        Intent next;
        if (step == 1) {
            next = new Intent(this, ShapeStepActivity.class);
            next.putExtra(EXTRA_STEP, 2);
            next.putExtra(EXTRA_M, selected);
        } else if (step == 2) {
            next = new Intent(this, ShapeStepActivity.class);
            next.putExtra(EXTRA_STEP, 3);
            next.putExtra(EXTRA_M, chosenM);
            next.putExtra(EXTRA_K, selected);
        } else {
            next = new Intent(this, MatMulActivity.class);
            next.putExtra(EXTRA_M, chosenM);
            next.putExtra(EXTRA_K, chosenK);
            next.putExtra(EXTRA_N, selected);
        }
        startActivity(next);
    }

    private static boolean contains(int[] values, int target) {
        for (int v : values) {
            if (v == target) {
                return true;
            }
        }
        return false;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED, selected);
    }
}
