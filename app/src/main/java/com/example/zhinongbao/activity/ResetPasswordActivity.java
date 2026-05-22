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
