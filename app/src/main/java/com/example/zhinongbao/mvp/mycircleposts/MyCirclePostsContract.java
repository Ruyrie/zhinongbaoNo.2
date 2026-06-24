package com.example.zhinongbao.mvp.mycircleposts;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

public interface MyCirclePostsContract {
    interface View extends BaseView<Presenter> {
        void showPosts(List<Article> posts, String currentUser);
        void showToast(String message);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        void clearInvalidPosts();
        int getCircleLikeCount(int articleId);
        int getCommentCount(int articleId);
        boolean isCircleLiked(int articleId);
        boolean isFollowing(String author);
        int getUserRole(String username);
        void toggleCircleLike(int articleId);
        void followUser(String author);
    }
}
