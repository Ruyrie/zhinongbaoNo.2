package com.example.zhinongbao.mvp.sellermine;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.ArticleRepository;
import com.example.zhinongbao.repository.OrderRepository;
import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【卖家我的 / Seller Mine】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1. 构造时拿到当前登录用户名，创建三个 Repository。
 *   2. start()：取卖家资料 + 今日/本月/累计营业额 + 各状态订单数 + 资讯列表，回调 View 渲染。
 *   3. switchToBuyer()：把当前活跃身份切成买家并重启首页。
 *   4. 资讯点赞相关：查询/切换点赞状态、取点赞数与评论数。
 * 数据来源：UserRepository（用户/身份）、OrderRepository（营业额/订单数）、
 *   ArticleRepository（资讯/点赞），Repository 内部经 ContentProvider 访问 SQLite；本类不直接碰数据库。
 * 配合的文件：接口 = SellerMineContract；View = fragment/SellerMineFragment；模型 = Order、User、Article。
 * 在 MVP 数据流中的位置：View 与 Repository 之间的业务中枢。
 * 提示：在 IDE 里搜索「卖家我的」可看本组相关文件。
 * ============================================================
 */
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
        this.username = userRepository.getLoggedUser(); // 当前登录用户名
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        // 一次性把卖家资料、营业额、订单角标、资讯列表都取好并回调 View 渲染
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
