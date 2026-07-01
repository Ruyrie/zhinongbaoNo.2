package com.example.zhinongbao.mvp.password;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【重置密码 / Reset Password】Presenter（业务逻辑）
 * 整体逻辑（submit 的校验顺序）：非空 → 至少 6 位 → 两次一致 →
 *   不能与旧密码相同 → 写库 → 提示成功并跳登录页。
 * 数据来源：走 UserRepository；目标 username 由构造时（上一步）传入。
 * 配合的文件：接口 = ResetPasswordContract；View = ResetPasswordActivity；
 *   上一步 = ForgotPasswordActivity。
 * 提示：在 IDE 里搜索「重置密码」可看本组相关文件。
 * ============================================================
 */
public class ResetPasswordPresenter implements ResetPasswordContract.Presenter {
    private final ResetPasswordContract.View view;
    private final UserRepository repository;
    private final String username; // 目标用户（上一步已确定）

    public ResetPasswordPresenter(Context context, ResetPasswordContract.View view, String username) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.username = username;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        // 无需初始化
    }

    // 校验新密码并保存，成功后回登录页
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
