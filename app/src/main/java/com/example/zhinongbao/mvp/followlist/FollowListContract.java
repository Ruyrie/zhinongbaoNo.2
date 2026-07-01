package com.example.zhinongbao.mvp.followlist;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

import java.util.List;

/**
 * ============================================================
 * 【关注/粉丝列表 / Follow List】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来显示普通名单(showUsers) 或 关注名单的两个 tab(showFollowing)。
 *   - Presenter：由 View 调用来取用户昵称/头像/店铺名、判断关注状态、切换关注。
 * 数据流向：View → 调 Presenter 方法 → Presenter 操作 ArticleRepository + UserRepository →
 *   回调 View 的 showUsers/showFollowing。
 * 配合的文件：View 实现 = FollowListActivity；Presenter 实现 = FollowListPresenter。
 * 提示：在 IDE 里搜索「关注列表」可看本组相关文件。
 * ============================================================
 */
public interface FollowListContract {
    // View：Presenter 用这些方法把名单「显示」到界面上
    interface View extends BaseView<Presenter> {
        void showUsers(List<String> users, String currentUser); // 显示单一名单（粉丝/获赞等）
        void showFollowing(List<String> userFollows, List<String> storeFollows, String currentUser); // 显示关注（用户/店铺两组）
    }

    // Presenter：View 用这些方法取数据或切换关注
    interface Presenter extends BasePresenter {
        String getNickname(String username);                 // 取昵称
        String getAvatarUri(String username);                // 取头像 URI
        String getStoreName(String username);                // 取店铺名
        boolean isStoreAccount(String username);             // 是否为可用卖家身份的账号
        boolean isFollowing(String username, boolean store); // 我是否已关注（store 区分用户/店铺）
        void toggleFollow(String username, boolean store);   // 关注/取消关注
    }
}
