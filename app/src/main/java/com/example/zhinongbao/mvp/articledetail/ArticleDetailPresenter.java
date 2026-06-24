package com.example.zhinongbao.mvp.articledetail;

import android.content.Context;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.Comment;
import com.example.zhinongbao.repository.ArticleRepository;

public class ArticleDetailPresenter implements ArticleDetailContract.Presenter {
    private final ArticleDetailContract.View view;
    private final ArticleRepository repository;
    private final int articleId;
    private final String currentUser;
    private Article article;

    public ArticleDetailPresenter(Context context, ArticleDetailContract.View view, int articleId) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.articleId = articleId;
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        repository.incrementReadCount(articleId);
        refresh();
    }

    @Override
    public void refresh() {
        article = repository.getArticleById(articleId);
        if (article == null) {
            view.closePage();
            return;
        }
        view.showArticle(article, currentUser, repository.isFollowing(currentUser, article.author));
        refreshComments();
        refreshLikeState();
    }

    @Override
    public void refreshComments() {
        view.showComments(repository.getComments(articleId, currentUser));
    }

    @Override
    public void refreshLikeState() {
        if (isCircleArticle()) {
            view.showLikeState(repository.isCirclePostLiked(currentUser, articleId),
                    repository.getCirclePostLikeCount(articleId));
        } else {
            view.showLikeState(repository.isArticleLiked(currentUser, articleId),
                    repository.getArticleLikeCount(articleId));
        }
    }

    @Override
    public void deleteArticle() {
        repository.deleteArticle(articleId);
        view.showToast("作品已删除");
        view.closePage();
    }

    @Override
    public void toggleFollow() {
        if (currentUser == null || currentUser.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (article == null || currentUser.equals(article.author)) {
            return;
        }
        if (repository.isFollowing(currentUser, article.author)) {
            repository.unfollowUser(currentUser, article.author);
        } else {
            repository.followUser(currentUser, article.author);
        }
        view.showFollowState(repository.isFollowing(currentUser, article.author));
    }

    @Override
    public void toggleArticleLike() {
        if (currentUser == null || currentUser.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (isCircleArticle()) {
            if (repository.isCirclePostLiked(currentUser, articleId)) {
                repository.unlikeCirclePost(currentUser, articleId);
            } else {
                repository.likeCirclePost(currentUser, articleId);
            }
        } else if (repository.isArticleLiked(currentUser, articleId)) {
            repository.unlikeArticle(currentUser, articleId);
        } else {
            repository.likeArticle(currentUser, articleId);
        }
        refreshLikeState();
    }

    @Override
    public void submitComment(String content) {
        if (currentUser == null || currentUser.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (content == null || content.isEmpty()) {
            return;
        }
        repository.addComment(articleId, currentUser, content);
        refreshComments();
    }

    @Override
    public void deleteComment(Comment comment) {
        repository.deleteComment(comment.id);
        refreshComments();
    }

    @Override
    public void likeComment(int commentId) {
        repository.likeComment(currentUser, commentId);
    }

    @Override
    public void unlikeComment(int commentId) {
        repository.unlikeComment(currentUser, commentId);
    }

    private boolean isCircleArticle() {
        if (article == null) {
            article = repository.getArticleById(articleId);
        }
        return article != null && "农友圈".equals(article.category);
    }
}
