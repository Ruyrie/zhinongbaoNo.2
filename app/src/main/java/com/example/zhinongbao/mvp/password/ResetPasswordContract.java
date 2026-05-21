package com.example.zhinongbao.mvp.password;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface ResetPasswordContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);
        void openLogin();
    }

    interface Presenter extends BasePresenter {
        void submit(String newPassword, String confirmPassword);
    }
}
