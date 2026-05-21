package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.mvp.register.RegisterContract;
import com.example.zhinongbao.mvp.register.RegisterPresenter;

/** 注册界面（复用为添加账号界面） */
public class RegisterActivity extends BaseMvpActivity<RegisterContract.Presenter> implements RegisterContract.View {

    private EditText etUsername, etPassword, etPhone;
    private RadioGroup rgRole;

    // 是否从账号管理进入（添加账号模式）
    private boolean isAddMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        new RegisterPresenter(this, this);
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
        int selectedRole = User.ROLE_BUYER;
        if (rgRole != null && rgRole.getCheckedRadioButtonId() == R.id.rbSeller) {
            selectedRole = User.ROLE_SELLER;
        }
        presenter.register(username, password, phone, selectedRole, isAddMode);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }

    @Override
    public void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
