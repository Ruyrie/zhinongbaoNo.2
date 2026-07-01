package com.example.zhinongbao.mvp.headline;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【头条 / Headline】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1) 构造时创建 ArticleRepository、取当前登录用户名，并 setPresenter 绑定 View。
 *   2) start()/refresh() 取全部文章回调 showArticles（分类过滤在 View 端做）。
 *   3) toggleArticleLike：未登录则忽略，否则按当前状态切换赞/取消。
 * 数据来源：本类不直接碰数据库，通过 ArticleRepository 访问（Repository 内部经
 *   ContentProvider 访问 SQLite）。
 * 配合的文件：接口 HeadlineContract；View 实现 HeadlineFragment；
 *   数据访问 repository/ArticleRepository；模型 model/Article。
 * 在 MVP 数据流中的位置：Presenter 层（View → Presenter → Repository → ContentProvider → SQLite → 回调 View）。
 * 提示：在 IDE 里搜索「头条」可看本组相关文件。
 * ============================================================
 */
public class HeadlinePresenter implements HeadlineContract.Presenter {
    private final HeadlineContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;

    public HeadlinePresenter(Context context, HeadlineContract.View view) {
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
        view.showArticles(repository.getArticles(), currentUser);
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
