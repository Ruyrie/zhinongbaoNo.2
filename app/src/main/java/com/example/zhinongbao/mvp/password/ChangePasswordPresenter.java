package com.example.zhinongbao.mvp.password;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【修改密码 / Change Password】Presenter（业务逻辑）
 * 整体逻辑（save 的校验顺序）：非空 → 至少 6 位 → 两次输入一致 →
 *   新密码不能和旧密码相同 → 写库 → 提示成功并关闭页面。
 * 数据来源：走 UserRepository（内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 = ChangePasswordContract；View = ChangePasswordActivity。
 * 提示：在 IDE 里搜索「修改密码」可看本组相关文件。
 * ============================================================
 */
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
        // 无需初始化
    }

    // 校验新密码并保存
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
