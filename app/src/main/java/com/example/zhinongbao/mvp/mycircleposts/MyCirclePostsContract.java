package com.example.zhinongbao.mvp.mycircleposts;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

public interface MyCirclePostsContract {
    interface View extends BaseView<Presenter> {
        void showPosts(List<Article> posts, String currentUser);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        int getArticleLikeCount(int articleId);
        int getCommentCount(int articleId);
        boolean isArticleLiked(int articleId);
        boolean isFollowing(String author);
        int getUserRole(String username);
        void toggleArticleLike(int articleId);
        void followUser(String author);
    }
}
