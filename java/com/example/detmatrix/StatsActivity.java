package com.example.detmatrix;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

/**
 * Statistics screen: the last 10 times plus the average of the last 100,
 * kept separately for each of the two modes.
 */
public class StatsActivity extends Activity {

    static final String EXTRA_MODE = "mode";

    private static final int RECENT_N = 10;
    private static final int AVG_N = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        final int mode = getIntent().getIntExtra(EXTRA_MODE, StatsStore.MODE_DET);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ((TextView) findViewById(R.id.mode_name)).setText(mode == StatsStore.MODE_DET
                ? R.string.stats_mode_det : R.string.stats_mode_matmul);

        int total = StatsStore.count(this, mode);
        View body = findViewById(R.id.stats_body);
        TextView empty = (TextView) findViewById(R.id.empty_note);

        if (total == 0) {
            empty.setVisibility(View.VISIBLE);
            body.setVisibility(View.GONE);
            return;
        }
        empty.setVisibility(View.GONE);
        body.setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.avg_title))
                .setText(getString(R.string.stats_avg_title, AVG_N));
        ((TextView) findViewById(R.id.avg_value))
                .setText(formatSeconds(StatsStore.average(this, mode, AVG_N)));
        ((TextView) findViewById(R.id.total_value)).setText(
                getString(R.string.stats_total) + "：" + total + " " + getString(R.string.stats_unit));
        ((TextView) findViewById(R.id.recent_title))
                .setText(getString(R.string.stats_recent_title, RECENT_N));

        long[] recent = StatsStore.recent(this, mode, RECENT_N);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recent.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(String.format(Locale.US, "%d.   %s", i + 1, formatSeconds(recent[i])));
        }
        ((TextView) findViewById(R.id.recent_list)).setText(sb.toString());
    }

    private String formatSeconds(long millis) {
        if (millis < 0L) {
            return getString(R.string.stats_none);
        }
        return String.format(Locale.US, "%.1f 秒", millis / 1000.0);
    }
}
