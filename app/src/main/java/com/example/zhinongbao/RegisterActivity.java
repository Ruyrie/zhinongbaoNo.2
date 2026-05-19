package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.User;

/** 注册界面（复用为添加账号界面） */
public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etPhone;
    private RadioGroup rgRole;
    private DataManager dm;

    // 是否从账号管理进入（添加账号模式）
    private boolean isAddMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dm = DataManager.getInstance(this);
        isAddMode = getIntent().getBooleanExtra("add_mode", false);

        etUsername = findViewById(R.id.etRegUsername);
        etPassword = findViewById(R.id.etRegPassword);
        etPhone = findViewById(R.id.etRegPhone);
        rgRole = findViewById(R.id.rgRole);
        Button btnRegister = findViewById(R.id.btnRegister);

        setTitle(isAddMode ? "添加账号" : "注册");

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!username.matches("^[a-zA-Z0-9\\-@_.]+$")) {
            Toast.makeText(this, "用户名只能包含字母、数字及-@_.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.isEmpty()) {
            if (phone.length() != 11 || !phone.matches("^1[3-9]\\d{9}$") || phone.matches("^(\\d)\\1{10}$")) {
                Toast.makeText(this, "请输入有效的11位手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dm.isPhoneBound(phone)) {
                Toast.makeText(this, "该手机号已被注册或绑定，请更换手机号", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int selectedRole = User.ROLE_BUYER;
        if (rgRole != null && rgRole.getCheckedRadioButtonId() == R.id.rbSeller) {
            selectedRole = User.ROLE_SELLER;
        }

        boolean ok = dm.register(username, password, phone, selectedRole);
        if (!ok) {
            Toast.makeText(this, "该用户名已被使用，请更换用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();

        if (isAddMode) {
            // 账号管理模式，直接返回
            finish();
        } else {
            // 注册完成后自动登录并进入主界面
            dm.setLoggedUser(username);
            dm.setActiveRole(selectedRole);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
