package com.example.zhinongbao.mvp.register;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【注册 / Register】Contract（接口约定）
 * 约定内容：
 *   - View：弹提示、弹「账号已注册」对话框、关闭页面、跳主页。
 *   - Presenter：register(用户名,密码,手机号,角色,是否添加模式)。
 * 关键概念：该页面复用为两种场景 —— 普通注册(注册完直接登录进主页) 与
 *   账号管理里的「添加账号」(addMode=true，注册完只关闭页面不登录)。
 * 配合的文件：View 实现 = RegisterActivity；Presenter 实现 = RegisterPresenter；
 *   数据访问 = repository/UserRepository；模型 = model/User。
 * 提示：在 IDE 里搜索「注册」可看本组相关文件。
 * ============================================================
 */
public interface RegisterContract {
    // View：Presenter 用这些方法更新注册界面
    interface View extends BaseView<Presenter> {
        void showToast(String message);                    // 弹提示
        void showAccountRegisteredDialog(String username); // 账号已存在：提示去登录
        void closePage();                                  // 关闭页面（添加账号模式用）
        void goMain();                                     // 注册并登录后进主页
    }

    // Presenter：View 用它触发注册业务
    interface Presenter extends BasePresenter {
        void register(String username, String password, String phone, int role, boolean addMode);
    }
}
