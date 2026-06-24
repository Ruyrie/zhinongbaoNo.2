package com.example.zhinongbao.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zhinongbao.R;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
    }
}
