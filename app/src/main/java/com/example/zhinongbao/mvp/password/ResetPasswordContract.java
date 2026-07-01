package com.example.zhinongbao.mvp.password;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【重置密码 / Reset Password】Contract（接口约定）
 * 约定内容：View（弹提示、跳登录页）；Presenter（submit：校验并保存新密码）。
 * 关键概念：目标用户名在上一步（忘记密码）已确定，由 Presenter 构造时传入，
 *   所以这里 submit 只需新密码与确认密码。
 * 配合的文件：View 实现 = ResetPasswordActivity；Presenter 实现 = ResetPasswordPresenter；
 *   上一步 = ForgotPasswordActivity；数据访问 = repository/UserRepository。
 * 提示：在 IDE 里搜索「重置密码」可看本组相关文件。
 * ============================================================
 */
public interface ResetPasswordContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message); // 弹提示
        void openLogin();               // 重置成功后跳登录页
    }

    interface Presenter extends BasePresenter {
        void submit(String newPassword, String confirmPassword); // 校验并保存新密码
    }
}
