package com.example.zhinongbao.mvp.password;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

public class ChangePasswordPresenter implements ChangePasswordContract.Presenter {
    private final ChangePasswordContract.View view;
    private final UserRepository repository;

    public ChangePasswordPresenter(Context context, ChangePasswordContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void save(String username, String newPwd, String confirmPwd) {
        if (newPwd.isEmpty()) {
            view.showToast("密码不能为空");
            return;
        }
        if (newPwd.length() < 6) {
            view.showToast("密码至少6位");
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            view.showToast("两次密码不一致");
            return;
        }
        if (repository.isSameAsOldPassword(username, newPwd)) {
            view.showToast("新密码不能和旧密码相同");
            return;
        }
        repository.changePassword(username, newPwd);
        view.showToast("密码修改成功");
        view.closePage();
    }
}
