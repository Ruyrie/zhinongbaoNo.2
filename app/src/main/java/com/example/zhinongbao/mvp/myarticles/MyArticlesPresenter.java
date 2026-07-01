package com.example.zhinongbao.mvp.myarticles;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【我的文章 / My Articles】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1) 构造时创建 ArticleRepository、取当前登录用户名；targetAuthor 为空则默认看自己。
 *   2) start()/refresh() 取该作者的文章回调 showArticles。
 *   3) toggleArticleLike：未登录忽略，否则按状态切换赞/取消。
 * 数据来源：本类不直接碰数据库，通过 ArticleRepository 访问（Repository 内部经
 *   ContentProvider 访问 SQLite）。
 * 配合的文件：接口 MyArticlesContract；View 实现 MyArticlesActivity；
 *   数据访问 repository/ArticleRepository；模型 model/Article。
 * 在 MVP 数据流中的位置：Presenter 层（View → Presenter → Repository → ContentProvider → SQLite → 回调 View）。
 * 提示：在 IDE 里搜索「我的文章」可看本组相关文件。
 * ============================================================
 */
public class MyArticlesPresenter implements MyArticlesContract.Presenter {
    private final MyArticlesContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;
    private final String targetAuthor;

    public MyArticlesPresenter(Context context, MyArticlesContract.View view) {
        this(context, view, null);
    }

    public MyArticlesPresenter(Context context, MyArticlesContract.View view, String targetAuthor) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.targetAuthor = targetAuthor == null || targetAuthor.isEmpty() ? this.currentUser : targetAuthor;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showArticles(repository.getArticlesByAuthor(targetAuthor), currentUser);
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
