package com.example.zhinongbao.mvp.myarticles;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

public interface MyArticlesContract {
    interface View extends BaseView<Presenter> {
        void showArticles(List<Article> articles, String currentUser);
        void openArticleDetail(int articleId);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        int getArticleLikeCount(int articleId);
        int getCommentCount(int articleId);
        boolean isArticleLiked(int articleId);
        void toggleArticleLike(int articleId);
    }
}
