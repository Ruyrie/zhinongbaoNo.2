package com.example.zhinongbao.mvp.accountmanager;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【账号管理 / AccountManager】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：构造时创建 UserRepository 并注入 View；
 *   start/refresh 取全部账号回调 showUsers；isCurrentUser 比对登录用户名；
 *   hideUserFromHistory 移除后刷新列表。
 * 数据来源：走 repository/UserRepository；Repository 内部经 ContentProvider
 *   访问 SQLite，本类不直接碰数据库。
 * 配合的文件：接口约定 = AccountManagerContract；View = AccountManagerActivity；模型 = model/User。
 * 在 MVP 数据流中的位置：Presenter（业务层），承上（View）启下（Repository）。
 * 提示：在 IDE 里搜索「账号管理」可看本组相关文件。
 * ============================================================
 */
public class AccountManagerPresenter implements AccountManagerContract.Presenter {
    private final AccountManagerContract.View view;     // 关联的界面
    private final UserRepository repository;            // 用户数据访问入口

    public AccountManagerPresenter(Context context, AccountManagerContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showUsers(repository.getUsers());
    }

    @Override
    public boolean isCurrentUser(String username) {
        return username != null && username.equals(repository.getLoggedUser());
    }

    @Override
    public void hideUserFromHistory(String username) {
        repository.hideUserFromHistory(username);
        refresh();
    }
}
