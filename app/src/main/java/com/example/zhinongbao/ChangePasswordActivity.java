package com.example.zhinongbao;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.password.ChangePasswordContract;
import com.example.zhinongbao.mvp.password.ChangePasswordPresenter;

/** 修改密码界面 */
public class ChangePasswordActivity extends BaseMvpActivity<ChangePasswordContract.Presenter>
        implements ChangePasswordContract.View {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        new ChangePasswordPresenter(this, this);

        String username = getIntent().getStringExtra("username");

        ((TextView) findViewById(R.id.tvChangeUsername)).setText("账号：" + username);

        EditText etNew = findViewById(R.id.etNewPassword);
        EditText etConfirm = findViewById(R.id.etConfirmPassword);
        Button btnSave = findViewById(R.id.btnSavePassword);

        btnSave.setOnClickListener(v -> {
            String newPwd = etNew.getText().toString().trim();
            String confirmPwd = etConfirm.getText().toString().trim();
            presenter.save(username, newPwd, confirmPwd);
        });
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }
}
