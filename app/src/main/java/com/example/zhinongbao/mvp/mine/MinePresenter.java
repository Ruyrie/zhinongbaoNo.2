package com.example.zhinongbao.mvp.mine;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.ArticleRepository;
import com.example.zhinongbao.repository.OrderRepository;
import com.example.zhinongbao.repository.UserRepository;

import java.util.List;

public class MinePresenter implements MineContract.Presenter {
    private final MineContract.View view;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final OrderRepository orderRepository;

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
