package com.example.zhinongbao.mvp.search;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

public interface SearchContract {
    interface View extends BaseView<Presenter> {
        void showAllArticles(List<Article> articles, String currentUser);
    }

    interface Presenter extends BasePresenter {
        int getArticleLikeCount(int articleId);
        int getCommentCount(int articleId);
        boolean isArticleLiked(int articleId);
        void toggleArticleLike(int articleId);
    }
}
