package com.example.zhinongbao.mvp.mycircleposts;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

public class MyCirclePostsPresenter implements MyCirclePostsContract.Presenter {
    private final MyCirclePostsContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;

    public MyCirclePostsPresenter(Context context, MyCirclePostsContract.View view) {
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
        view.showPosts(repository.getCirclePostsByAuthor(currentUser), currentUser);
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
    public boolean isFollowing(String author) {
        return repository.isFollowing(currentUser, author);
    }

    @Override
    public int getUserRole(String username) {
        return repository.getUserRole(username);
    }

    @Override
    public void toggleArticleLike(int articleId) {
        if (repository.isArticleLiked(currentUser, articleId)) {
            repository.unlikeArticle(currentUser, articleId);
        } else {
            repository.likeArticle(currentUser, articleId);
        }
    }

    @Override
    public void followUser(String author) {
        repository.followUser(currentUser, author);
    }
}
