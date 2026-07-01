package com.example.zhinongbao.mvp.password;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【修改密码 / Change Password】Contract（接口约定）
 * 约定内容：View（弹提示、关页面）；Presenter（save：校验并保存新密码）。
 * 配合的文件：View 实现 = ChangePasswordActivity；Presenter 实现 = ChangePasswordPresenter；
 *   数据访问 = repository/UserRepository。
 * 提示：在 IDE 里搜索「修改密码」可看本组相关文件。相关还有「忘记密码」「重置密码」。
 * ============================================================
 */
public interface ChangePasswordContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message); // 弹提示
        void closePage();               // 改成功后关闭页面
    }

    interface Presenter extends BasePresenter {
        void save(String username, String newPwd, String confirmPwd); // 校验并保存新密码
    }
}
