package com.example.zhinongbao.mvp.password;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【忘记密码 / Forgot Password】Presenter（业务逻辑）
 * 整体逻辑（nextStep）：账号非空 → 验证码非空 → 验证码比对(忽略大小写) →
 *   反查账号是否存在 → openResetPassword。任一步失败会提示并按需刷新验证码。
 * 数据来源：走 UserRepository（内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 = ForgotPasswordContract；View = ForgotPasswordActivity；
 *   下一步 = ResetPasswordActivity。
 * 提示：在 IDE 里搜索「忘记密码」可看本组相关文件。
 * ============================================================
 */
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
        // 无需初始化
    }

    // 校验账号与验证码，通过后带真实用户名进入重置页
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
