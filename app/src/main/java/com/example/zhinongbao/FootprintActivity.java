package com.example.zhinongbao;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class FootprintActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_footprint);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        findViewById(R.id.tvFootprintBack).setOnClickListener(v -> finish());
    }
}
