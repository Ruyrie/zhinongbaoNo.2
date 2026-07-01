package com.example.zhinongbao.mvp.mine;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【我的 / Mine】Contract（接口约定）
 * 约定内容：
 *   - View：渲染用户资料与统计、渲染订单状态角标、切换角色后重建主界面。
 *   - Presenter：取当前登录用户、切换买家/卖家角色。
 * 配合的文件：View 实现 = MineFragment；Presenter 实现 = MinePresenter；
 *   数据访问 = UserRepository / ArticleRepository / OrderRepository；模型 = model/User、model/Order。
 * 在 MVP 数据流中的位置：接口层，连接 View 与 Presenter。
 * 提示：在 IDE 里搜索「我的」可看本组相关文件。
 * ============================================================
 */
public interface MineContract {
    interface View extends BaseView<Presenter> {
        // 渲染用户资料、头像、粉丝/关注/获赞及角色切换按钮
        void renderUser(String username, String nickname, String signature, String avatarUri,
                int followers, int following, int likes, boolean sellerActive, boolean canSwitchSeller);
        // 渲染订单各状态数量角标
        void renderOrderBadges(int pendingCount, int paidCount, int shippedCount,
                int reviewingCount, int refundCount);
        void restartMain();     // 切换角色后重建主界面
    }

    interface Presenter extends BasePresenter {
        String getCurrentUser();    // 取当前登录用户名
        void switchRole();          // 切换买家/卖家角色
    }
}
