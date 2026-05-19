package com.example.zhinongbao;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zhinongbao.data.DataManager;

public class SettingsActivity extends AppCompatActivity {

    private DataManager dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        dm = DataManager.getInstance(this);

        findViewById(R.id.tvAccountManager)
                .setOnClickListener(v -> startActivity(new Intent(this, AccountManagerActivity.class)));

        findViewById(R.id.tvLogout).setOnClickListener(v -> confirmLogout());

        findViewById(R.id.tvAddArticle)
                .setOnClickListener(v -> startActivity(new Intent(this, AddArticleActivity.class)));

        findViewById(R.id.tvAddProduct)
                .setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
    }

    private void confirmLogout() {
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        tvTitle.setText("退出登录");
        tvMessage.setText("确定要退出登录吗？");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            dm.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        dialog.show();
    }
}
