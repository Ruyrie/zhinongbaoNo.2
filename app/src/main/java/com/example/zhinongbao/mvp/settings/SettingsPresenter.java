package com.example.zhinongbao.mvp.settings;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【设置 / Settings】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：构造时创建 UserRepository 并注入 View；
 *   logout 调用仓库清除登录状态后，通知 View 跳回登录页。
 * 数据来源：走 repository/UserRepository；Repository 内部经 ContentProvider
 *   访问 SQLite，本类不直接碰数据库。
 * 配合的文件：接口约定 = SettingsContract；View = SettingsActivity。
 * 在 MVP 数据流中的位置：Presenter（业务层），承上（View）启下（Repository）。
 * 提示：在 IDE 里搜索「设置」可看本组相关文件。
 * ============================================================
 */
public class SettingsPresenter implements SettingsContract.Presenter {
    private final SettingsContract.View view;       // 关联的界面
    private final UserRepository repository;        // 用户数据访问入口

    public SettingsPresenter(Context context, SettingsContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void logout() {
        repository.logout();
        view.goLogin();
    }
}
