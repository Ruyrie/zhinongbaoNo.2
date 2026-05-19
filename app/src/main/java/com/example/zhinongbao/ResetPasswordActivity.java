package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zhinongbao.data.DataManager;

public class ResetPasswordActivity extends AppCompatActivity {

    private String username;
    private String displayAccount;
    private DataManager dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        dm = DataManager.getInstance(this);

        username = getIntent().getStringExtra("username");
        displayAccount = getIntent().getStringExtra("display_account");

        TextView tvAccount = findViewById(R.id.tvResetAccount);
        tvAccount.setText("重置账号: " + displayAccount);

        EditText etNewPassword = findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnSubmitReset = findViewById(R.id.btnSubmitReset);

        btnSubmitReset.setOnClickListener(v -> {
            String newPwd = etNewPassword.getText().toString().trim();
            String confirmPwd = etConfirmPassword.getText().toString().trim();

            if (newPwd.isEmpty()) {
                Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPwd.length() < 6) {
                Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPwd.equals(confirmPwd)) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dm.isSameAsOldPassword(username, newPwd)) {
                Toast.makeText(this, "新密码不能和旧密码相同", Toast.LENGTH_SHORT).show();
                return;
            }

            dm.changePassword(username, newPwd);
            Toast.makeText(this, "密码重置成功，请使用新密码登录", Toast.LENGTH_LONG).show();

            // 跳转回登录页面
            Intent intent = new Intent(this, LoginActivity.class);
            // 清理任务栈，保证用户无法后退到重置页面
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
