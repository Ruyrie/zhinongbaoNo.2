package com.example.zhinongbao.mvp.password;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

public class ResetPasswordPresenter implements ResetPasswordContract.Presenter {
    private final ResetPasswordContract.View view;
    private final UserRepository repository;
    private final String username;

    public ResetPasswordPresenter(Context context, ResetPasswordContract.View view, String username) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.username = username;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void submit(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            view.showToast("请输入新密码");
            return;
        }
        if (newPassword.length() < 6) {
            view.showToast("密码至少6位");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            view.showToast("两次密码不一致");
            return;
        }
        if (repository.isSameAsOldPassword(username, newPassword)) {
            view.showToast("新密码不能和旧密码相同");
            return;
        }

        repository.changePassword(username, newPassword);
        view.showToast("密码重置成功，请使用新密码登录");
        view.openLogin();
    }
}
