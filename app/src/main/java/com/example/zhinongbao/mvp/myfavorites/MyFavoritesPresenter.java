package com.example.zhinongbao.mvp.myfavorites;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【我的收藏 / My Favorites】Presenter（业务逻辑）
 * 整体逻辑：refresh 从 Repository 取该用户点赞的文章回调 View；clearInvalidArticles
 *   清理已删除文章并按清理数量给出提示；getXxx 系列供适配器查询点赞数/评论数/是否已赞；
 *   toggleArticleLike 按当前状态点赞或取消点赞。
 * 数据来源：走 repository/ArticleRepository，Repository 内部通过 ContentProvider
 *   访问 SQLite；本类不直接操作数据库。
 * 配合的文件：接口约定 mvp/myfavorites/MyFavoritesContract；View 实现 =
 *   MyFavoritesActivity；列表适配器 adapter/ArticleAdapter；模型 = model/Article。
 * 在 MVP 数据流中的位置：业务层。
 * 提示：在 IDE 里搜索「我的收藏」可看本组相关文件。
 * ============================================================
 */
public class MyFavoritesPresenter implements MyFavoritesContract.Presenter {
    private final MyFavoritesContract.View view;   // 对应的界面
    private final ArticleRepository repository;    // 文章数据访问
    private final String currentUser;              // 当前登录用户

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
