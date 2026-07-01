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

/**
 * ============================================================
 * 【忘记密码 / Forgot Password】View（Activity）
 * 整体逻辑：onCreate 里 refreshCaptcha() 生成图形验证码并记下正确值
 *   realCaptcha；点验证码图片可刷新；点「下一步」→ presenter.nextStep(
 *   账号, 用户输入验证码, realCaptcha)；成功回调 openResetPassword 跳转。
 * 数据来源：验证码由本地 utils/CaptchaUtils 生成（离线，不发短信）；
 *   账号是否存在的判断由 Presenter → UserRepository 完成。
 * 配合的文件：接口 ForgotPasswordContract；业务 ForgotPasswordPresenter；
 *   验证码工具 utils/CaptchaUtils；布局 activity_forgot_password.xml；
 *   下一步 ResetPasswordActivity。
 * 在 MVP 中的位置：View 层。
 * 提示：在 IDE 里搜索「忘记密码」可看本组相关文件。
 * ============================================================
 */
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
