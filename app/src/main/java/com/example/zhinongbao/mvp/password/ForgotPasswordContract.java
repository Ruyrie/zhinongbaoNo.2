package com.example.zhinongbao.mvp.password;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface ForgotPasswordContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);
        void refreshCaptcha();
        void clearCaptchaInput();
        void openResetPassword(String username, String displayAccount);
    }

    interface Presenter extends BasePresenter {
        void nextStep(String account, String inputCaptcha, String realCaptcha);
    }
}
