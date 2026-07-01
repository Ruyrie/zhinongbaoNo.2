package com.example.zhinongbao.mvp.login;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【登录 / Login】Contract（接口约定）
 * 约定内容：
 *   - View：跳主页、弹提示、弹「账号未注册」对话框、弹「买家/卖家」版本选择框。
 *   - Presenter：登录、选择登录角色。
 * 关键概念：一个账号可能同时是买家和卖家(ROLE_BOTH)，登录成功后需让用户
 *   选择进入哪个版本，因此有 showRoleSelection / selectRole。
 * 配合的文件：View 实现 = LoginActivity；Presenter 实现 = LoginPresenter；
 *   数据访问 = repository/UserRepository；模型 = model/User。
 * 提示：在 IDE 里搜索「登录」可看本组相关文件。
 * ============================================================
 */
public interface LoginContract {
    // View：Presenter 用这些方法更新登录界面
    interface View extends BaseView<Presenter> {
        void goMain();                              // 跳转主页面
        void showToast(String message);             // 弹提示
        void showUnregisteredDialog(String account);// 账号不存在时提示去注册
        void showRoleSelection(String username);    // 双角色账号：选择买家/卖家版本
    }

    // Presenter：View（用户点击）用这些方法触发业务
    interface Presenter extends BasePresenter {
        void login(String account, String password);   // 校验并登录
        void selectRole(String username, int role);    // 确定进入的角色版本
    }
}
