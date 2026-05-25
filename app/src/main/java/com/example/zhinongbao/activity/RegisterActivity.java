package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.mvp.register.RegisterContract;
import com.example.zhinongbao.mvp.register.RegisterPresenter;

/** 注册界面（复用为添加账号界面） */
public class RegisterActivity extends BaseMvpActivity<RegisterContract.Presenter> implements RegisterContract.View {

    private EditText etUsername, etPassword, etPhone;
    private ImageView ivPasswordToggle;
    private RadioGroup rgRole;
    private boolean passwordVisible = false;

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
        ivPasswordToggle = findViewById(R.id.ivRegPasswordToggle);
        rgRole = findViewById(R.id.rgRole);
        Button btnRegister = findViewById(R.id.btnRegister);
        configureRoleIcon(R.id.rbBuyer, R.drawable.zhucemaijia1);
        configureRoleIcon(R.id.rbSeller, R.drawable.zhucemaijia2);
        findViewById(R.id.ivRegisterBack).setOnClickListener(v -> finish());
        String prefillUsername = getIntent().getStringExtra("prefill_username");
        if (prefillUsername != null && !prefillUsername.trim().isEmpty()) {
            etUsername.setText(prefillUsername.trim());
            etUsername.setSelection(etUsername.getText().length());
        }

        setTitle(isAddMode ? "添加账号" : "注册");

        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());
        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void configureRoleIcon(int radioButtonId, int drawableRes) {
        RadioButton radioButton = findViewById(radioButtonId);
        Drawable icon = ContextCompat.getDrawable(this, drawableRes);
        if (radioButton == null || icon == null) {
            return;
        }
        int size = dp(34);
        icon.setBounds(0, 0, size, size);
        radioButton.setCompoundDrawables(icon, null, null, null);
        radioButton.setCompoundDrawablePadding(dp(10));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
    public void showAccountRegisteredDialog(String username) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        android.widget.TextView btnCancel = view.findViewById(R.id.btnDialogCancel);
        android.widget.TextView btnConfirm = view.findViewById(R.id.btnDialogConfirm);

        tvTitle.setText("账号已注册");
        tvMessage.setText("账号 \"" + username + "\" 已经存在\n可以直接登录继续使用支农宝");
        btnCancel.setText("换个账号");
        btnConfirm.setText("直接登录");
        btnConfirm.setBackgroundResource(R.drawable.bg_auth_green_button);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            etUsername.requestFocus();
            etUsername.selectAll();
        });
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("prefill_username", username);
            startActivity(intent);
            finish();
        });
        dialog.show();
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
