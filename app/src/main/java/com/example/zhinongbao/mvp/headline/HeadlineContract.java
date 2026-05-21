package com.example.zhinongbao.mvp.headline;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

public interface HeadlineContract {
    interface View extends BaseView<Presenter> {
        void showArticles(List<Article> articles, String currentUser);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        int getArticleLikeCount(int articleId);
        int getCommentCount(int articleId);
        boolean isArticleLiked(int articleId);
        void toggleArticleLike(int articleId);
    }
}
