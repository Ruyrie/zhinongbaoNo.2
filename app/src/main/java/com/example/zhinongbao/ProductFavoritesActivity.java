package com.example.zhinongbao;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ProductFavoritesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_favorites);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        findViewById(R.id.tvProductFavBack).setOnClickListener(v -> finish());
    }
}
