package com.example.zhinongbao.mvp.password;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

public class ForgotPasswordPresenter implements ForgotPasswordContract.Presenter {
    private final ForgotPasswordContract.View view;
    private final UserRepository repository;

    public ForgotPasswordPresenter(Context context, ForgotPasswordContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void nextStep(String account, String inputCaptcha, String realCaptcha) {
        if (account == null || account.isEmpty()) {
            view.showToast("请输入用户名或手机号");
            return;
        }
        if (inputCaptcha == null || inputCaptcha.isEmpty()) {
            view.showToast("请输入验证码");
            return;
        }
        if (realCaptcha == null || !inputCaptcha.equalsIgnoreCase(realCaptcha)) {
            view.showToast("验证码错误，请重新输入");
            view.refreshCaptcha();
            view.clearCaptchaInput();
            return;
        }

        String targetUsername = repository.findUsernameByAccount(account);
        if (targetUsername == null || targetUsername.isEmpty()) {
            view.showToast("未找到该用户，请检查输入的用户名或手机号");
            view.refreshCaptcha();
            return;
        }

        view.openResetPassword(targetUsername, account);
    }
}
