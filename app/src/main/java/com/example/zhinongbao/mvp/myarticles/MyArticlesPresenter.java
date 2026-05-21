package com.example.zhinongbao.mvp.myarticles;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

public class MyArticlesPresenter implements MyArticlesContract.Presenter {
    private final MyArticlesContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;

    public MyArticlesPresenter(Context context, MyArticlesContract.View view) {
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
        view.showArticles(repository.getArticlesByAuthor(currentUser), currentUser);
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
