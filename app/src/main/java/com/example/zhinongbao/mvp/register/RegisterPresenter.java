package com.example.zhinongbao.mvp.register;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【注册 / Register】Presenter（业务逻辑）
 * 整体逻辑（register 的校验顺序）：
 *   1) 用户名/密码非空；2) 用户名字符合法(字母数字及 -@_.)；3) 密码≥6 位；
 *   4) 账号是否已存在；5) 若填了手机号，校验格式并检查是否被占用   ^从字符串开头开始
 *  1第一位必须是1
 * [3-9]第二位必须是3～9
 * \d{9}后面必须还有9位数字
 * $到字符串结尾；
 * 拒绝11个相同数字
 *   6) 写入数据库；7) 注册成功后：添加模式只关页面，普通模式记录登录并进主页。
 * 数据来源：全部走 UserRepository（内部通过 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 = RegisterContract；View = RegisterActivity；
 *   数据访问 = repository/UserRepository；模型 = model/User。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「注册」可看本组相关文件。
 * ============================================================
 */
public class RegisterPresenter implements RegisterContract.Presenter {
    private final RegisterContract.View view;    // 回调界面
    private final UserRepository repository;      // 用户数据访问入口

    public RegisterPresenter(Context context, RegisterContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        // 注册页无需初始化数据
    }

    // 注册主流程：逐项校验通过后写库；addMode 决定注册完是关页面还是登录进主页
    @Override
    public void register(String username, String password, String phone, int role, boolean addMode) {
        if (username.isEmpty() || password.isEmpty()) {
            view.showToast("用户名和密码不能为空");
            return;
        }
        if (!username.matches("^[a-zA-Z0-9\\-@_.]+$")) {
            view.showToast("用户名只能包含字母、数字及-@_.");
            return;
        }
        if (password.length() < 6) {
            view.showToast("密码至少6位");
            return;
        }
        String existingUsername = repository.findUsernameByAccount(username);
        if (existingUsername != null) {
            view.showAccountRegisteredDialog(existingUsername);
            return;
        }
        if (!phone.isEmpty()) {
            if (phone.length() != 11 || !phone.matches("^1[3-9]\\d{9}$") || phone.matches("^(\\d)\\1{10}$")) {
                view.showToast("请输入有效的11位手机号");
                return;
            }
            if (repository.isPhoneBound(phone)) {
                view.showToast("该手机号已被注册或绑定，请更换手机号");
                return;
            }
        }
        if (!repository.register(username, password, phone, role)) {
            view.showToast("该用户名已被使用，请更换用户名");
            return;
        }
        view.showToast("注册成功");
        if (addMode) {
            view.closePage();
        } else {
            repository.setLoggedUser(username);
            repository.setActiveRole(role);
            view.goMain();
        }
    }
}
