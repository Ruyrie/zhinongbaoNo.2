package com.example.zhinongbao.mvp.accountmanager;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.User;

import java.util.List;

/**
 * ============================================================
 * 【账号管理 / AccountManager】Contract（接口约定）
 * 约定内容：
 *   - View：showUsers 刷新账号列表、showCannotRemoveCurrentUser 提示不可删当前账号。
 *   - Presenter：refresh 重新加载、isCurrentUser 判断是否当前登录账号、
 *     hideUserFromHistory 从本机历史移除账号。
 * 配合的文件：View 实现 = AccountManagerActivity；Presenter 实现 = AccountManagerPresenter；
 *   数据访问 = repository/UserRepository；模型 = model/User。
 * 在 MVP 数据流中的位置：接口层，连接 View 与 Presenter。
 * 提示：在 IDE 里搜索「账号管理」可看本组相关文件。
 * ============================================================
 */
public interface AccountManagerContract {
    interface View extends BaseView<Presenter> {
        void showUsers(List<User> users);           // 刷新账号列表
        void showCannotRemoveCurrentUser();         // 提示：不能删除当前登录账号
    }

    interface Presenter extends BasePresenter {
        void refresh();                             // 重新加载账号列表
        boolean isCurrentUser(String username);     // 是否为当前登录账号
        void hideUserFromHistory(String username);  // 从本机登录历史中移除账号
    }
}
