package com.example.zhinongbao.mvp.agricircle;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

public class AgriCirclePresenter implements AgriCircleContract.Presenter {
    private final AgriCircleContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;

    public AgriCirclePresenter(Context context, AgriCircleContract.View view) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.showHeaderAvatar(currentUser, repository.getAvatarUri(currentUser));
        loadLatest();
    }

    @Override
    public void loadLatest() {
        view.showPosts(repository.getCirclePosts(), currentUser);
    }

    @Override
    public void loadFollowing() {
        view.showPosts(repository.getCirclePostsByFollowing(currentUser), currentUser);
    }

    @Override
    public void loadMine() {
        view.showPosts(repository.getCirclePostsByAuthor(currentUser), currentUser);
    }

    @Override
    public int getCircleLikeCount(int articleId) {
        return repository.getCirclePostLikeCount(articleId);
    }

    @Override
    public int getCommentCount(int articleId) {
        return repository.getCommentCount(articleId);
    }

    @Override
    public boolean isCircleLiked(int articleId) {
        return repository.isCirclePostLiked(currentUser, articleId);
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
    public void toggleCircleLike(int articleId) {
        if (repository.isCirclePostLiked(currentUser, articleId)) {
            repository.unlikeCirclePost(currentUser, articleId);
        } else {
            repository.likeCirclePost(currentUser, articleId);
        }
    }

    @Override
    public void followUser(String author) {
        repository.followUser(currentUser, author);
    }
}
