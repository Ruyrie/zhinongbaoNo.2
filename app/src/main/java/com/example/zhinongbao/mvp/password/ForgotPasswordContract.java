package com.example.zhinongbao.mvp.password;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【忘记密码 / Forgot Password】Contract（接口约定）
 * 约定内容：
 *   - View：弹提示、刷新图形验证码、清空验证码输入、跳到重置密码页。
 *   - Presenter：nextStep（校验账号与验证码，通过则进入重置页）。
 * 关键概念：验证码是本地图形验证码（见 utils/CaptchaUtils），realCaptcha 是
 *   界面当前生成的正确值，与用户输入 inputCaptcha 比对。
 * 配合的文件：View 实现 = ForgotPasswordActivity；Presenter 实现 = ForgotPasswordPresenter；
 *   下一步 = ResetPasswordActivity；数据访问 = repository/UserRepository。
 * 提示：在 IDE 里搜索「忘记密码」可看本组相关文件。
 * ============================================================
 */
public interface ForgotPasswordContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);      // 弹提示
        void refreshCaptcha();               // 重新生成图形验证码
        void clearCaptchaInput();            // 清空验证码输入框
        void openResetPassword(String username, String displayAccount); // 跳到重置密码页
    }

    interface Presenter extends BasePresenter {
        void nextStep(String account, String inputCaptcha, String realCaptcha); // 校验后进入下一步
    }
}
