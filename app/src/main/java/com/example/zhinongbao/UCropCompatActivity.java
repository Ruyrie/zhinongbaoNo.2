package com.example.zhinongbao;

import android.os.Bundle;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.yalantis.ucrop.UCropActivity;

public class UCropCompatActivity extends UCropActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View content = getWindow().getDecorView().findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
