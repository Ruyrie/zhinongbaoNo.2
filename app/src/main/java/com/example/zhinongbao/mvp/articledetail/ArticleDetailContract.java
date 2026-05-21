package com.example.zhinongbao.mvp.articledetail;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.Comment;

import java.util.List;

public interface ArticleDetailContract {
    interface View extends BaseView<Presenter> {
        void showArticle(Article article, String currentUser, boolean following);
        void showComments(List<Comment> comments);
        void showLikeState(boolean liked, int likeCount);
        void showFollowState(boolean following);
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void refresh();
        void refreshComments();
        void refreshLikeState();
        void deleteArticle();
        void toggleFollow();
        void toggleArticleLike();
        void submitComment(String content);
        void deleteComment(Comment comment);
        void likeComment(int commentId);
        void unlikeComment(int commentId);
    }
}
