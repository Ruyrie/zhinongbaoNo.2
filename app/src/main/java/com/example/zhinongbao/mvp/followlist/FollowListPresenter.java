package com.example.zhinongbao.mvp.followlist;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;
import com.example.zhinongbao.repository.UserRepository;

import java.util.List;

/**
 * ============================================================
 * 【关注/粉丝列表 / Follow List】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1) 构造时同时创建 ArticleRepository（关注关系数据）与 UserRepository（用户资料），取当前登录用户名。
 *   2) start() 按 type 分流："likes"取给某人文章点赞的人、"followers"取粉丝、
 *      "following"取其关注（分用户/店铺两组，回调 showFollowing），其余默认取关注。
 *   3) 提供昵称/头像/店铺名查询给 View 渲染每一行。
 *   4) toggleFollow：按 store 与当前关注状态，调用关注/取关（用户或店铺）的对应方法。
 * 数据来源：本类不直接碰数据库，通过 ArticleRepository + UserRepository 访问
 *   （Repository 内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 FollowListContract；View 实现 FollowListActivity；
 *   数据访问 repository/ArticleRepository、repository/UserRepository。
 * 在 MVP 数据流中的位置：Presenter 层（View → Presenter → Repository → ContentProvider → SQLite → 回调 View）。
 * 提示：在 IDE 里搜索「关注列表」可看本组相关文件。
 * ============================================================
 */
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
