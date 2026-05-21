package com.example.zhinongbao.mvp.myfavorites;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

public class MyFavoritesPresenter implements MyFavoritesContract.Presenter {
    private final MyFavoritesContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;

    public MyFavoritesPresenter(Context context, MyFavoritesContract.View view) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showArticles(repository.getLikedArticles(currentUser), currentUser);
    }

    @Override
    public void clearInvalidArticles() {
        int cleared = repository.clearInvalidLikedArticles(currentUser);
        if (cleared > 0) {
            view.showToast("成功清理 " + cleared + " 篇失效文章");
            refresh();
        } else {
            view.showToast("没有需要清理的失效文章");
        }
    }

    @Override
    public int getArticleLikeCount(int articleId) {
        return repository.getArticleLikeCount(articleId);
    }

    @Override
    public int getCommentCount(int articleId) {
        return repository.getCommentCount(articleId);
    }

    @Override
    public boolean isArticleLiked(int articleId) {
        return repository.isArticleLiked(currentUser, articleId);
    }

    @Override
    public void toggleArticleLike(int articleId) {
        if (currentUser == null || currentUser.isEmpty()) {
            return;
        }
        if (repository.isArticleLiked(currentUser, articleId)) {
            repository.unlikeArticle(currentUser, articleId);
        } else {
            repository.likeArticle(currentUser, articleId);
        }
    }
}
