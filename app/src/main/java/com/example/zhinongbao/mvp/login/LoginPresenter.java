package com.example.zhinongbao.mvp.login;

import android.content.Context;

import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【登录 / Login】Presenter（业务逻辑）
 * 整体逻辑：
 *   start()：若本地已有登录用户，直接进主页（免登录）。
 *   login()：先校验非空 → 查账号是否存在(findUsernameByAccount) →
 *     校验密码 → 若账号同时是买卖双角色则让用户选版本，否则记录登录状态后进主页。
 * 数据来源：全部走 UserRepository（内部通过 ContentProvider 访问 SQLite），
 *   本类不直接操作数据库。账号可用用户名或手机号登录（由仓库解析）。
 * 配合的文件：接口 = LoginContract；View = LoginActivity；
 *   数据访问 = repository/UserRepository；模型 = model/User。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「登录」可看本组相关文件。
 * ============================================================
 */
public class LoginPresenter implements LoginContract.Presenter {
    private final LoginContract.View view;       // 回调界面
    private final UserRepository repository;      // 用户数据访问入口

    public LoginPresenter(Context context, LoginContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    // 进入登录页时：若已登录则直接跳主页（记住登录状态）
    @Override
    public void start() {
        if (repository.getLoggedUser() != null) {
            view.goMain();
        }
    }

    // 登录主流程：空值校验 → 账号是否存在 → 密码校验 → 角色处理
    @Override
    public void login(String account, String password) {
        if (account.isEmpty() || password.isEmpty()) {
            view.showToast("用户名和密码不能为空");
            return;
        }
        // 账号（用户名或手机号）反查真实用户名；查不到说明未注册
        String targetUsername = repository.findUsernameByAccount(account);
        if (targetUsername == null || targetUsername.isEmpty()) {
            view.showUnregisteredDialog(account);
            return;
        }
        User user = repository.login(account, password); // 返回 null 表示密码错
        if (user == null) {
            view.showToast("密码错误");
            return;
        }
        int role = repository.getUserRole(user.username);
        if (role == User.ROLE_BOTH) {
            view.showRoleSelection(user.username); // 双角色：让用户选买家/卖家版本
        } else {
            // 单角色：记录登录状态与当前角色后进主页
            repository.setLoggedUser(user.username);
            repository.setActiveRole(role);
            view.goMain();
        }
    }

    @Override
    public void selectRole(String username, int role) {
        repository.setLoggedUser(username);
        repository.setActiveRole(role);
        view.goMain();
    }
}
