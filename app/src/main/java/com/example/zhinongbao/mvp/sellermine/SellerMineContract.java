package com.example.zhinongbao.mvp.sellermine;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

public interface SellerMineContract {
    interface View extends BaseView<Presenter> {
        void renderSeller(String username, String storeName, String nickname, String avatarUri,
                double todayRevenue, double monthRevenue, double totalRevenue);
        void renderNews(List<Article> articles);
        void restartMain();
    }

    interface Presenter extends BasePresenter {
        void switchToBuyer();
        boolean isArticleLiked(int articleId);
        void toggleArticleLike(int articleId);
        int getArticleLikeCount(int articleId);
        int getCommentCount(int articleId);
    }
}
