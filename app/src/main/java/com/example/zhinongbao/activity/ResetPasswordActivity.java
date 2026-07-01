package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.password.ResetPasswordContract;
import com.example.zhinongbao.mvp.password.ResetPasswordPresenter;

/**
 * ============================================================
 * 【重置密码 / Reset Password】View（Activity）
 * 整体逻辑：onCreate 取上一步传入的 username / display_account，显示账号；
 *   点「提交」→ presenter.submit(新密码,确认密码)；成功回调 openLogin
 *   （清空任务栈跳登录页）。
 * 数据来源：本类不碰数据库，改密由 Presenter → UserRepository 完成。
 * 配合的文件：接口 ResetPasswordContract；业务 ResetPasswordPresenter；
 *   布局 activity_reset_password.xml；上一步 ForgotPasswordActivity；
 *   成功后跳 LoginActivity。
 * 在 MVP 中的位置：View 层。
 * 提示：在 IDE 里搜索「重置密码」可看本组相关文件。
 * ============================================================
 */
public class ResetPasswordActivity extends BaseMvpActivity<ResetPasswordContract.Presenter>
        implements ResetPasswordContract.View {

    private String username;
    private String displayAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        username = getIntent().getStringExtra("username");
        displayAccount = getIntent().getStringExtra("display_account");
        new ResetPasswordPresenter(this, this, username).start();

        TextView tvAccount = findViewById(R.id.tvResetAccount);
        tvAccount.setText("重置账号: " + displayAccount);

        EditText etNewPassword = findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnSubmitReset = findViewById(R.id.btnSubmitReset);

        btnSubmitReset.setOnClickListener(v -> {
            String newPwd = etNewPassword.getText().toString().trim();
            String confirmPwd = etConfirmPassword.getText().toString().trim();
            presenter.submit(newPwd, confirmPwd);
        });
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
