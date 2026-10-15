package com.example.detmatrix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/** Home screen: pick a practice mode. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button detButton = (Button) findViewById(R.id.mode_det);
        detButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DetActivity.class));
            }
        });

        Button matMulButton = (Button) findViewById(R.id.mode_matmul);
        matMulButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MatMulActivity.class));
            }
        });
    }
}
