package com.example.zhinongbao.mvp.agricircle;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

public interface AgriCircleContract {
    interface View extends BaseView<Presenter> {
        void showPosts(List<Article> posts, String currentUser);
        void showHeaderAvatar(String currentUser, String avatarUri);
    }

    interface Presenter extends BasePresenter {
        void loadLatest();
        void loadFollowing();
        void loadMine();
        int getCircleLikeCount(int articleId);
        int getCommentCount(int articleId);
        boolean isCircleLiked(int articleId);
        boolean isFollowing(String author);
        int getUserRole(String username);
        void toggleCircleLike(int articleId);
        void followUser(String author);
    }
}
