package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.password.ChangePasswordContract;
import com.example.zhinongbao.mvp.password.ChangePasswordPresenter;

/**
 * ============================================================
 * 【修改密码 / Change Password】View（Activity）
 * 整体逻辑：onCreate 取 Intent 传入的 username，绑定输入框；点「保存」→
 *   presenter.save(username,新密码,确认密码)；成功后回调 closePage 关页面。
 * 数据来源：本类不碰数据库，改密逻辑由 Presenter → UserRepository 完成。
 * 配合的文件：接口 ChangePasswordContract；业务 ChangePasswordPresenter；
 *   布局 activity_change_password.xml；通常由 AccountManagerActivity 跳入。
 * 在 MVP 中的位置：View 层。
 * 提示：在 IDE 里搜索「修改密码」可看本组相关文件。
 * ============================================================
 */
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
