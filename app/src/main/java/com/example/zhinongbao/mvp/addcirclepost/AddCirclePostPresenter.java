package com.example.zhinongbao.mvp.addcirclepost;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【发农友圈动态 / Add Circle Post】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1) 构造时创建 ArticleRepository 并 setPresenter 绑定 View。
 *   2) submit：校验内容非空 → 调 repository.addCirclePost 写入 → 弹「发布成功」→ 关闭页面。
 * 数据来源：本类不直接碰数据库，通过 ArticleRepository 写入（Repository 内部经
 *   ContentProvider 写 SQLite）。
 * 配合的文件：接口 AddCirclePostContract；View 实现 AddCirclePostActivity；
 *   数据访问 repository/ArticleRepository。
 * 在 MVP 数据流中的位置：Presenter 层（View → Presenter → Repository → ContentProvider → SQLite）。
 * 提示：在 IDE 里搜索「发农友圈」可看本组相关文件。
 * ============================================================
 */
public class AddCirclePostPresenter implements AddCirclePostContract.Presenter {
    private final AddCirclePostContract.View view;
    private final ArticleRepository repository;

    public AddCirclePostPresenter(Context context, AddCirclePostContract.View view) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void submit(String content, String imageUris) {
        if (content == null || content.isEmpty()) {
            view.showToast("请输入动态内容");
            return;
        }
        repository.addCirclePost(content, imageUris);
        view.showToast("发布成功！");
        view.closePage();
    }
}
