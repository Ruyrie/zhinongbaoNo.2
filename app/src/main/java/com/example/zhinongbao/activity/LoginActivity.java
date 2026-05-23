package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.mvp.login.LoginContract;
import com.example.zhinongbao.mvp.login.LoginPresenter;
import com.example.zhinongbao.utils.DialogUtils;

/** 登录界面 */
public class LoginActivity extends BaseMvpActivity<LoginContract.Presenter> implements LoginContract.View {

    private EditText etUsername, etPassword;
    private ImageView ivPasswordToggle;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new LoginPresenter(this, this).start();
        if (isFinishing())
            return;

        setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        ivPasswordToggle = findViewById(R.id.ivLoginPasswordToggle);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvGoRegister);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> doLogin());
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            String account = etUsername.getText().toString().trim();
            if (!account.isEmpty()) {
                intent.putExtra("prefill_username", account);
            }
            startActivity(intent);
        });
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivPasswordToggle.setImageResource(R.drawable.chakan_auth);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivPasswordToggle.setImageResource(R.drawable.weichakan_auth);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        presenter.login(username, password);
    }

    @Override
    public void showRoleSelection(String username) {
        String[] options = { "登录买家版本", "登录卖家版本" };
        DialogUtils.showRoleSelection(this, "请选择登录版本", options, which -> {
                    int selectedRole = (which == 0) ? User.ROLE_BUYER : User.ROLE_SELLER;
                    presenter.selectRole(username, selectedRole);
                });
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
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra("prefill_username", account);
            startActivity(intent);
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
