package com.example.zhinongbao.mvp.mycircleposts;

import android.content.Context;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.repository.ArticleRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【我的动态 / My Circle Posts】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1) 构造时创建 ArticleRepository、取当前登录用户名，记录 favoritesMode（是否为「我点赞的」模式）。
 *   2) refresh() 按模式取数：favoritesMode 取我点赞的动态，否则取我发布的动态，回调 showPosts。
 *   3) clearInvalidPosts() 清理已删动态的点赞记录，并按结果弹提示后刷新。
 *   4) toggleCircleLike 处理点赞：自己的动态不允许点赞（弹提示），其余按状态切换。
 * 数据来源：本类不直接碰数据库，通过 ArticleRepository 访问（Repository 内部经
 *   ContentProvider 访问 SQLite）。
 * 配合的文件：接口 MyCirclePostsContract；View 实现 MyCirclePostsActivity；
 *   数据访问 repository/ArticleRepository；模型 model/Article。
 * 在 MVP 数据流中的位置：Presenter 层（View → Presenter → Repository → ContentProvider → SQLite → 回调 View）。
 * 提示：在 IDE 里搜索「我的动态」可看本组相关文件。
 * ============================================================
 */
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
            view.showPosts(repository.getLikedCirclePosts(currentUser), currentUser);
        } else {
            view.showPosts(repository.getCirclePostsByAuthor(currentUser), currentUser);
        }
    }

    @Override
    public void clearInvalidPosts() {
        int cleared = repository.clearInvalidCircleLikes(currentUser);
        if (cleared > 0) {
            view.showToast("成功清理 " + cleared + " 条失效动态");
            refresh();
        } else {
            view.showToast("没有需要清理的失效动态");
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
        if (isOwnCirclePost(articleId)) {
            repository.unlikeCirclePost(currentUser, articleId);
            view.showToast("不能给自己的动态点赞");
            return;
        }
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

    private boolean isOwnCirclePost(int articleId) {
        Article article = repository.getArticleById(articleId);
        return article != null && currentUser != null && currentUser.equals(article.author);
    }
}
