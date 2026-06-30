package com.example.zhinongbao.mvp.followlist;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;
import com.example.zhinongbao.repository.UserRepository;

import java.util.List;

public class FollowListPresenter implements FollowListContract.Presenter {
    private final FollowListContract.View view;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final String type;
    private final String username;
    private final String currentUser;

    public FollowListPresenter(Context context, FollowListContract.View view, String type, String username) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.articleRepository = new ArticleRepository(appContext);
        this.userRepository = new UserRepository(appContext);
        this.type = type;
        this.username = username;
        this.currentUser = userRepository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        List<String> users;
        if ("likes".equals(type)) {
            users = articleRepository.getUsersWhoLikedArticlesBy(username);
        } else if ("followers".equals(type)) {
            users = articleRepository.getFollowers(username);
        } else if ("following".equals(type)) {
            view.showFollowing(articleRepository.getFollowing(username),
                    articleRepository.getFollowingStores(username), currentUser);
            return;
        } else {
            users = articleRepository.getFollowing(username);
        }
        view.showUsers(users, currentUser);
    }

    @Override
    public String getNickname(String username) {
        return userRepository.getNickname(username);
    }

    @Override
    public String getAvatarUri(String username) {
        return userRepository.getAvatarUri(username);
    }

    @Override
    public String getStoreName(String username) {
        return userRepository.getStoreName(username);
    }

    @Override
    public boolean isStoreAccount(String username) {
        return userRepository.canUseSellerRole(username);
    }

    @Override
    public boolean isFollowing(String username, boolean store) {
        return store
                ? articleRepository.isFollowingStore(currentUser, username)
                : articleRepository.isFollowing(currentUser, username);
    }

    @Override
    public void toggleFollow(String username, boolean store) {
        if (store) {
            if (articleRepository.isFollowingStore(currentUser, username)) {
                articleRepository.unfollowStore(currentUser, username);
            } else {
                articleRepository.followStore(currentUser, username);
            }
        } else if (articleRepository.isFollowing(currentUser, username)) {
            articleRepository.unfollowUser(currentUser, username);
        } else {
            articleRepository.followUser(currentUser, username);
        }
    }
}
