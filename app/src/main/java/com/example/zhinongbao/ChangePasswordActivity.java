package com.example.zhinongbao;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zhinongbao.data.DataManager;

/** 修改密码界面 */
public class ChangePasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        String username = getIntent().getStringExtra("username");
        DataManager dm = DataManager.getInstance(this);

        ((TextView) findViewById(R.id.tvChangeUsername)).setText("账号：" + username);

        EditText etNew = findViewById(R.id.etNewPassword);
        EditText etConfirm = findViewById(R.id.etConfirmPassword);
        Button btnSave = findViewById(R.id.btnSavePassword);

        btnSave.setOnClickListener(v -> {
            String newPwd = etNew.getText().toString().trim();
            String confirmPwd = etConfirm.getText().toString().trim();
            if (newPwd.isEmpty()) {
                Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
