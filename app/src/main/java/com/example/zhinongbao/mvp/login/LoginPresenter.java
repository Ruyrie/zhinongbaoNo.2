package com.example.zhinongbao.mvp.login;

import android.content.Context;

import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.UserRepository;

public class LoginPresenter implements LoginContract.Presenter {
    private final LoginContract.View view;
    private final UserRepository repository;

    public LoginPresenter(Context context, LoginContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        if (repository.getLoggedUser() != null) {
            view.goMain();
        }
    }

    @Override
    public void login(String account, String password) {
        if (account.isEmpty() || password.isEmpty()) {
            view.showToast("用户名和密码不能为空");
            return;
        }
        String targetUsername = repository.findUsernameByAccount(account);
        if (targetUsername == null || targetUsername.isEmpty()) {
            view.showUnregisteredDialog(account);
            return;
        }
        User user = repository.login(account, password);
        if (user == null) {
            view.showToast("密码错误");
            return;
        }
        int role = repository.getUserRole(user.username);
        if (role == User.ROLE_BOTH) {
            view.showRoleSelection(user.username);
        } else {
            repository.setLoggedUser(user.username);
            repository.setActiveRole(role);
            view.goMain();
        }
    }

    @Override
    public void selectRole(String username, int role) {
        repository.setLoggedUser(username);
        repository.setActiveRole(role);
        view.goMain();
    }
}
