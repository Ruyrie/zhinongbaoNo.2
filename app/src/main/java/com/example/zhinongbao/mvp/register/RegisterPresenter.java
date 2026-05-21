package com.example.zhinongbao.mvp.register;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

public class RegisterPresenter implements RegisterContract.Presenter {
    private final RegisterContract.View view;
    private final UserRepository repository;

    public RegisterPresenter(Context context, RegisterContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void register(String username, String password, String phone, int role, boolean addMode) {
        if (username.isEmpty() || password.isEmpty()) {
            view.showToast("用户名和密码不能为空");
            return;
        }
        if (!username.matches("^[a-zA-Z0-9\\-@_.]+$")) {
            view.showToast("用户名只能包含字母、数字及-@_.");
            return;
        }
        if (password.length() < 6) {
            view.showToast("密码至少6位");
            return;
        }
        if (!phone.isEmpty()) {
            if (phone.length() != 11 || !phone.matches("^1[3-9]\\d{9}$") || phone.matches("^(\\d)\\1{10}$")) {
                view.showToast("请输入有效的11位手机号");
                return;
            }
            if (repository.isPhoneBound(phone)) {
                view.showToast("该手机号已被注册或绑定，请更换手机号");
                return;
            }
        }
        if (!repository.register(username, password, phone, role)) {
            view.showToast("该用户名已被使用，请更换用户名");
            return;
        }
        view.showToast("注册成功");
        if (addMode) {
            view.closePage();
        } else {
            repository.setLoggedUser(username);
            repository.setActiveRole(role);
            view.goMain();
        }
    }
}
