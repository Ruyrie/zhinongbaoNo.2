package com.example.zhinongbao.mvp.login;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface LoginContract {
    interface View extends BaseView<Presenter> {
        void goMain();
        void showToast(String message);
        void showUnregisteredDialog(String account);
        void showRoleSelection(String username);
    }

    interface Presenter extends BasePresenter {
        void login(String account, String password);
        void selectRole(String username, int role);
    }
}
