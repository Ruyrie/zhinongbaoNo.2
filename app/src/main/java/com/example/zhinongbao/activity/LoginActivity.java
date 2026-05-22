package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.mvp.login.LoginContract;
import com.example.zhinongbao.mvp.login.LoginPresenter;

/** 登录界面 */
public class LoginActivity extends BaseMvpActivity<LoginContract.Presenter> implements LoginContract.View {

    private EditText etUsername, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new LoginPresenter(this, this).start();
        if (isFinishing())
            return;

        setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvGoRegister);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> doLogin());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        presenter.login(username, password);
    }

    @Override
    public void showRoleSelection(String username) {
        String[] options = { "登录买家版本", "登录卖家版本" };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("请选择登录版本")
                .setItems(options, (dialog, which) -> {
                    int selectedRole = (which == 0) ? User.ROLE_BUYER : User.ROLE_SELLER;
                    presenter.selectRole(username, selectedRole);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void showUnregisteredDialog(String account) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        tvTitle.setText("账号未注册");
        tvMessage.setText("该账号 (" + account + ") 尚未注册\n是否立即前往注册？");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.TextView btnConfirm = view.findViewById(R.id.btnDialogConfirm);
        btnConfirm.setText("立即注册");
        btnConfirm.setBackgroundResource(R.drawable.bg_auth_button);

        view.findViewById(R.id.btnDialogCancel).setOnClickListener(btn -> dialog.dismiss());
        btnConfirm.setOnClickListener(btn -> {
            dialog.dismiss();
            startActivity(new Intent(this, RegisterActivity.class));
        });
        dialog.show();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
