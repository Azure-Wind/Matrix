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

/** Entry point of the matrix-multiplication wrong book: 9 rows x 3 columns = 27 size buttons. */
public class WrongShapesActivity extends Activity {

    private static final int COLS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrong_shapes);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        LinearLayout grid = (LinearLayout) findViewById(R.id.shape_grid);

        // Fixed cell width: weighted widths can collapse under a wrap_content
        // parent, so the size is computed from the screen width instead.
        int cellPx = shapeCellWidthPx();
        LinearLayout row = null;
        for (int i = 0; i < Shapes.COUNT; i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row);
            }
            row.addView(makeShapeButton(i, cellPx));
        }
    }

    /** Width of one size button so that COLS of them fit the screen exactly. */
    private int shapeCellWidthPx() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float widthDp = dm.widthPixels / dm.density;
        // Page padding 16dp each side, plus a 4dp margin on each side of a button.
        float perCol = (widthDp - 32f - COLS * 8f) / COLS;
        if (perCol > 140f) {
            perCol = 140f;
        }
        if (perCol < 84f) {
            perCol = 84f;
        }
        return Math.round(perCol * dm.density);
    }

    private Button makeShapeButton(final int index, int cellPx) {
        Button b = new Button(this);
        b.setText(Shapes.label(index));
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        b.setTypeface(null, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setAllCaps(false);
        b.setBackgroundResource(R.drawable.cell_bg_small);
        b.setTextColor(getResources().getColor(R.color.number));
        // Buttons carry a default minimum size; clear it so 0dp+weight works.
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(2f), 0, dp(2f), 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                cellPx, dp(56f));
        lp.setMargins(dp(4f), dp(4f), dp(4f), dp(4f));
        b.setLayoutParams(lp);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent next = new Intent(WrongShapesActivity.this, WrongListActivity.class);
                next.putExtra(WrongListActivity.EXTRA_IS_DET, false);
                next.putExtra(WrongListActivity.EXTRA_SHAPE, index);
                startActivity(next);
            }
        });
        return b;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
