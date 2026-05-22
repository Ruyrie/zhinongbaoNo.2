package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.password.ForgotPasswordContract;
import com.example.zhinongbao.mvp.password.ForgotPasswordPresenter;
import com.example.zhinongbao.utils.CaptchaUtils;

public class ForgotPasswordActivity extends BaseMvpActivity<ForgotPasswordContract.Presenter>
        implements ForgotPasswordContract.View {

    private EditText etAccount, etCaptcha;
    private ImageView ivCaptcha;
    private String realCaptcha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        new ForgotPasswordPresenter(this, this).start();

        etAccount = findViewById(R.id.etForgotAccount);
        etCaptcha = findViewById(R.id.etForgotCaptcha);
        ivCaptcha = findViewById(R.id.ivCaptcha);
        Button btnNextStep = findViewById(R.id.btnNextStep);

        refreshCaptcha();

        ivCaptcha.setOnClickListener(v -> refreshCaptcha());

        btnNextStep.setOnClickListener(v -> doNextStep());
    }

    @Override
    public void refreshCaptcha() {
        CaptchaUtils utils = CaptchaUtils.getInstance();
        Bitmap bitmap = utils.createBitmap();
        realCaptcha = utils.getCode();
        ivCaptcha.setImageBitmap(bitmap);
    }

    private void doNextStep() {
        String account = etAccount.getText().toString().trim();
        String inputCaptcha = etCaptcha.getText().toString().trim();
        presenter.nextStep(account, inputCaptcha, realCaptcha);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void clearCaptchaInput() {
        etCaptcha.setText("");
    }

    @Override
    public void openResetPassword(String username, String displayAccount) {
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("display_account", displayAccount);
        startActivity(intent);
        finish();
    }
}
