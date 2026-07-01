package com.example.zhinongbao.mvp.settings;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【设置 / Settings】Contract（接口约定）
 * 约定内容：
 *   - View：goLogin 跳回登录页。
 *   - Presenter：logout 退出登录。
 * 配合的文件：View 实现 = SettingsActivity；Presenter 实现 = SettingsPresenter；
 *   数据访问 = repository/UserRepository。
 * 在 MVP 数据流中的位置：接口层，连接 View 与 Presenter。
 * 提示：在 IDE 里搜索「设置」可看本组相关文件。
 * ============================================================
 */
public interface SettingsContract {
    interface View extends BaseView<Presenter> {
        void goLogin();     // 退出后跳回登录页
    }

    interface Presenter extends BasePresenter {
        void logout();      // 退出登录
    }
}
