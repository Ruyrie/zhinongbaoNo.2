package com.example.zhinongbao.mvp.sellermine;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.ArticleRepository;
import com.example.zhinongbao.repository.OrderRepository;
import com.example.zhinongbao.repository.UserRepository;

public class SellerMinePresenter implements SellerMineContract.Presenter {
    private final SellerMineContract.View view;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final OrderRepository orderRepository;
    private final String username;

    public SellerMinePresenter(Context context, SellerMineContract.View view) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.userRepository = new UserRepository(appContext);
        this.articleRepository = new ArticleRepository(appContext);
        this.orderRepository = new OrderRepository(appContext);
        this.username = userRepository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.renderSeller(username, userRepository.getStoreName(username), userRepository.getNickname(username),
                userRepository.getAvatarUri(username), orderRepository.getRevenueForSeller(username, "today"),
                orderRepository.getRevenueForSeller(username, "month"), orderRepository.getRevenueForSeller(username, "all"));
        view.renderOrderBadges(
                orderRepository.getSellerOrderCountByStatus(username, Order.STATUS_PENDING),
                orderRepository.getSellerOrderCountByStatus(username, Order.STATUS_PAID),
                orderRepository.getSellerOrderCountByStatus(username, Order.STATUS_SHIPPED),
                orderRepository.getSellerOrderCountByStatus(username, Order.STATUS_REFUND));
        view.renderNews(articleRepository.getArticles());
    }

    @Override
    public void switchToBuyer() {
        userRepository.setActiveRole(User.ROLE_BUYER);
        view.restartMain();
    }

    @Override
    public boolean isArticleLiked(int articleId) {
        return articleRepository.isArticleLiked(username, articleId);
    }

    @Override
    public void toggleArticleLike(int articleId) {
        if (articleRepository.isArticleLiked(username, articleId)) {
            articleRepository.unlikeArticle(username, articleId);
        } else {
            articleRepository.likeArticle(username, articleId);
        }
    }

    @Override
    public int getArticleLikeCount(int articleId) {
        return articleRepository.getArticleLikeCount(articleId);
    }

    @Override
    public int getCommentCount(int articleId) {
        return articleRepository.getCommentCount(articleId);
    }
}
