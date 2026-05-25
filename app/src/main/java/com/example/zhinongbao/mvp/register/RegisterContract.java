package com.example.zhinongbao.mvp.register;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface RegisterContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);
        void showAccountRegisteredDialog(String username);
        void closePage();
        void goMain();
    }

    interface Presenter extends BasePresenter {
        void register(String username, String password, String phone, int role, boolean addMode);
    }
}
