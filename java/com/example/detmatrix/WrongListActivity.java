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

import java.util.List;

/**
 * Contents of one wrong-answer collection: numbers only.
 *
 * Each number is coloured by {@link WrongBook#state}:
 *   white  not mastered yet
 *   red    the last redo was reported as wrong
 *   green  two correct redos in a row
 *
 * Tapping a number opens the timed redo screen for that problem.
 */
public class WrongListActivity extends Activity {

    static final String EXTRA_IS_DET = "isDet";
    static final String EXTRA_SHAPE = "shape";

    /** Four per row leaves room for two-digit numbers. */
    private static final int COLS = 4;

    private boolean isDet;
    private int shapeIndex;
    private String key;

    private LinearLayout numberGrid;
    private TextView titleView;
    private TextView subtitleView;
    private TextView emptyNote;
    private TextView tapHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrong_list);

        isDet = getIntent().getBooleanExtra(EXTRA_IS_DET, true);
        shapeIndex = getIntent().getIntExtra(EXTRA_SHAPE, 0);
        key = isDet ? WrongBook.KEY_DET : WrongBook.keyForShape(shapeIndex);

        numberGrid = (LinearLayout) findViewById(R.id.number_grid);
        titleView = (TextView) findViewById(R.id.list_title);
        subtitleView = (TextView) findViewById(R.id.list_subtitle);
        emptyNote = (TextView) findViewById(R.id.empty_note);
        tapHint = (TextView) findViewById(R.id.tap_hint);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        titleView.setText(isDet
                ? getString(R.string.wrong_title_det)
                : getString(R.string.stats_mode_matmul) + " " + Shapes.label(shapeIndex));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh on return from the redo screen so the colours are up to date.
        rebuild();
    }

    private void rebuild() {
        List<WrongBook.Item> items = WrongBook.list(this, key);
        int mastered = WrongBook.masteredCount(this, key);

        subtitleView.setText(getString(R.string.wrong_count, items.size())
                + "　·　" + getString(R.string.wrong_done_count, mastered, items.size()));

        numberGrid.removeAllViews();
        if (items.isEmpty()) {
            emptyNote.setVisibility(View.VISIBLE);
            tapHint.setVisibility(View.GONE);
            return;
        }
        emptyNote.setVisibility(View.GONE);
        tapHint.setVisibility(View.VISIBLE);

        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                numberGrid.addView(row);
            }
            row.addView(makeNumberButton(i, WrongBook.state(items.get(i))));
        }
    }

    private Button makeNumberButton(final int index, int state) {
        Button b = new Button(this);
        b.setText(String.valueOf(index + 1));
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
        b.setTypeface(null, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setAllCaps(false);

        int background;
        int textColor;
        if (state == WrongBook.STATE_GREEN) {
            background = R.drawable.chip_done;
            textColor = R.color.chip_text_selected;
        } else if (state == WrongBook.STATE_RED) {
            background = R.drawable.chip_wrong;
            textColor = R.color.chip_text_selected;
        } else {
            background = R.drawable.cell_bg_small;
            textColor = R.color.number;
        }
        b.setBackgroundResource(background);
        b.setTextColor(getResources().getColor(textColor));

        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(0, 0, 0, 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(64f), 1f);
        lp.setMargins(dp(5f), dp(5f), dp(5f), dp(5f));
        b.setLayoutParams(lp);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPractice(index);
            }
        });
        return b;
    }

    private void startPractice(int index) {
        List<WrongBook.Item> items = WrongBook.list(this, key);
        if (index < 0 || index >= items.size()) {
            return;
        }
        WrongBook.Item item = items.get(index);

        Intent intent;
        if (isDet) {
            intent = new Intent(this, DetActivity.class);
        } else {
            intent = new Intent(this, MatMulActivity.class);
            intent.putExtra(ShapeStepActivity.EXTRA_M, item.m);
            intent.putExtra(ShapeStepActivity.EXTRA_K, item.k);
            intent.putExtra(ShapeStepActivity.EXTRA_N, item.n);
            intent.putExtra(Practice.EXTRA_VALUES_B, item.b);
        }
        intent.putExtra(Practice.EXTRA_PRACTICE, true);
        intent.putExtra(Practice.EXTRA_WRONG_KEY, key);
        intent.putExtra(Practice.EXTRA_WRONG_INDEX, index);
        intent.putExtra(Practice.EXTRA_VALUES_A, item.a);
        startActivity(intent);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
