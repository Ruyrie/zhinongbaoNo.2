package com.example.zhinongbao.mvp.addarticle;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【发文章 / Add Article】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1) 构造时创建 ArticleRepository 并 setPresenter 绑定 View。
 *   2) submit：校验标题正文非空 → 调 repository.addArticle 写入 → 弹「发布成功」→ 关闭页面。
 * 数据来源：本类不直接碰数据库，通过 ArticleRepository 写入（Repository 内部经
 *   ContentProvider 写 SQLite）。
 * 配合的文件：接口 AddArticleContract；View 实现 AddArticleActivity；
 *   数据访问 repository/ArticleRepository；模型 model/Article。
 * 在 MVP 数据流中的位置：Presenter 层（View → Presenter → Repository → ContentProvider → SQLite）。
 * 提示：在 IDE 里搜索「发文章」可看本组相关文件。
 * ============================================================
 */
public class AddArticlePresenter implements AddArticleContract.Presenter {
    private final AddArticleContract.View view;
    private final ArticleRepository repository;

    public AddArticlePresenter(Context context, AddArticleContract.View view) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void submit(String title, String content, String coverUri, String category) {
        if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
            view.showToast("标题和内容不能为空");
            return;
        }
        repository.addArticle(title, content, coverUri, category);
        view.showToast("文章发布成功");
        view.closePage();
    }
}
