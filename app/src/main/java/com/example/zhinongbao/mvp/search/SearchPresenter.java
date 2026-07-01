package com.example.zhinongbao.mvp.search;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【头条搜索 / Search（文章）】Presenter（业务逻辑）
 * 整体逻辑：start() 一次性把所有文章交给 View，具体的关键词匹配在 View 端本地
 *   完成（数据量小）；点赞相关方法转发给仓库。
 * 数据来源：走 ArticleRepository（内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 = SearchContract；View = SearchActivity；模型 = model/Article。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「头条搜索」可看本组相关文件。
 * ============================================================
 */
public class SearchPresenter implements SearchContract.Presenter {
    private final SearchContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;

    public SearchPresenter(Context context, SearchContract.View view) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.showAllArticles(repository.getArticles(), currentUser);
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
