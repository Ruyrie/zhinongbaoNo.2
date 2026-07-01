package com.example.zhinongbao.mvp.mine;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.ArticleRepository;
import com.example.zhinongbao.repository.OrderRepository;
import com.example.zhinongbao.repository.UserRepository;

import java.util.List;

/**
 * ============================================================
 * 【我的 / Mine】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1. 构造时创建三个 Repository 并把自己注入 View。
 *   2. start 取登录用户，汇总资料与统计数据回调 renderUser，再算订单角标。
 *   3. renderOrderBadges 遍历订单按状态计数，加上待评价数量回调 View。
 *   4. switchRole 校验可切换后翻转角色，通知 View 重建主界面。
 * 数据来源：走 UserRepository / ArticleRepository / OrderRepository；
 *   Repository 内部经 ContentProvider 访问 SQLite，本类不直接碰数据库。
 * 配合的文件：接口约定 = MineContract；View = MineFragment；模型 = model/User、model/Order。
 * 在 MVP 数据流中的位置：Presenter（业务层），承上（View）启下（Repository）。
 * 提示：在 IDE 里搜索「我的」可看本组相关文件。
 * ============================================================
 */
public class MinePresenter implements MineContract.Presenter {
    private final MineContract.View view;                    // 关联的界面
    private final UserRepository userRepository;             // 用户资料数据
    private final ArticleRepository articleRepository;       // 关注/获赞等社交数据
    private final OrderRepository orderRepository;           // 订单数据

    public MinePresenter(Context context, MineContract.View view) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.userRepository = new UserRepository(appContext);
        this.articleRepository = new ArticleRepository(appContext);
        this.orderRepository = new OrderRepository(appContext);
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        String username = userRepository.getLoggedUser();
        view.renderUser(username, userRepository.getNickname(username), userRepository.getSignature(username),
                userRepository.getAvatarUri(username), articleRepository.getFollowersCount(username),
                articleRepository.getFollowingCount(username), articleRepository.getTotalLikesReceived(username),
                userRepository.getActiveRole() == User.ROLE_SELLER, userRepository.canUseSellerRole(username));
        renderOrderBadges(username);
    }

    private void renderOrderBadges(String username) {
        if (username == null) {
            view.renderOrderBadges(0, 0, 0, 0, 0);
            return;
        }

        int pendingCount = 0;
        int paidCount = 0;
        int shippedCount = 0;
        int refundCount = 0;
        List<Order> orders = orderRepository.getOrders(username);
        for (Order order : orders) {
            if (Order.STATUS_PENDING.equals(order.status)) {
                pendingCount++;
            } else if (Order.STATUS_PAID.equals(order.status)) {
                paidCount++;
            } else if (Order.STATUS_SHIPPED.equals(order.status)) {
                shippedCount++;
            } else if (Order.STATUS_REFUND.equals(order.status)) {
                refundCount++;
            }
        }

        int reviewingCount = orderRepository.getPendingReviewOrders(username).size();
        view.renderOrderBadges(pendingCount, paidCount, shippedCount, reviewingCount, refundCount);
    }

    @Override
    public String getCurrentUser() {
        return userRepository.getLoggedUser();
    }

    @Override
    public void switchRole() {
        String username = userRepository.getLoggedUser();
        if (!userRepository.canUseSellerRole(username)) {
            return;
        }
        boolean sellerActive = userRepository.getActiveRole() == User.ROLE_SELLER;
        userRepository.setActiveRole(sellerActive ? User.ROLE_BUYER : User.ROLE_SELLER);
        view.restartMain();
    }
}
