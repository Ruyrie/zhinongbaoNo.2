package com.example.zhinongbao;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.utils.CaptchaUtils;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etAccount, etCaptcha;
    private ImageView ivCaptcha;
    private DataManager dm;
    private String realCaptcha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        dm = DataManager.getInstance(this);

        etAccount = findViewById(R.id.etForgotAccount);
        etCaptcha = findViewById(R.id.etForgotCaptcha);
        ivCaptcha = findViewById(R.id.ivCaptcha);
        Button btnNextStep = findViewById(R.id.btnNextStep);

        refreshCaptcha();

        ivCaptcha.setOnClickListener(v -> refreshCaptcha());

        btnNextStep.setOnClickListener(v -> doNextStep());
    }

    private void refreshCaptcha() {
        CaptchaUtils utils = CaptchaUtils.getInstance();
        Bitmap bitmap = utils.createBitmap();
        realCaptcha = utils.getCode();
        ivCaptcha.setImageBitmap(bitmap);
    }

    private void doNextStep() {
        String account = etAccount.getText().toString().trim();
        String inputCaptcha = etCaptcha.getText().toString().trim();

        if (account.isEmpty()) {
            Toast.makeText(this, "请输入用户名或手机号", Toast.LENGTH_SHORT).show();
            return;
        }

        if (inputCaptcha.isEmpty()) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputCaptcha.equalsIgnoreCase(realCaptcha)) {
            Toast.makeText(this, "验证码错误，请重新输入", Toast.LENGTH_SHORT).show();
            refreshCaptcha();
            etCaptcha.setText("");
            return;
        }

        String targetUsername = dm.findUsernameByAccount(account);
        if (targetUsername == null || targetUsername.isEmpty()) {
            Toast.makeText(this, "未找到该用户，请检查输入的用户名或手机号", Toast.LENGTH_SHORT).show();
            refreshCaptcha();
            return;
        }

        // 验证通过，进入下一个页面
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        intent.putExtra("username", targetUsername);
        intent.putExtra("display_account", account);
        startActivity(intent);
        finish();
    }
}
