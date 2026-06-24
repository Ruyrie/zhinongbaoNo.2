package com.example.zhinongbao.mvp.mycircleposts;

import android.content.Context;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.repository.ArticleRepository;

import java.util.ArrayList;
import java.util.List;

public class MyCirclePostsPresenter implements MyCirclePostsContract.Presenter {
    private final MyCirclePostsContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;
    private final boolean favoritesMode;

    public MyCirclePostsPresenter(Context context, MyCirclePostsContract.View view) {
        this(context, view, false);
    }

    public MyCirclePostsPresenter(Context context, MyCirclePostsContract.View view, boolean favoritesMode) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.favoritesMode = favoritesMode;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        if (favoritesMode) {
            view.showPosts(getLikedCirclePosts(), currentUser);
        } else {
            view.showPosts(repository.getCirclePostsByAuthor(currentUser), currentUser);
        }
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

    private List<Article> getLikedCirclePosts() {
        List<Article> likedPosts = new ArrayList<>();
        for (Article article : repository.getCirclePosts()) {
            if (repository.isCirclePostLiked(currentUser, article.id)) {
                likedPosts.add(article);
            }
        }
        return likedPosts;
    }
}
