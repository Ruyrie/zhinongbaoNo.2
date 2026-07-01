package com.example.zhinongbao.mvp.addcirclepost;

import android.content.Context;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【发农友圈动态 / Add Circle Post】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1) 构造时创建 ArticleRepository 并 setPresenter 绑定 View。
 *   2) submit：校验内容非空 → 调 repository.addCirclePost 写入 → 弹「发布成功」→ 关闭页面。
 *   3) 编辑模式：loadPost 取原动态回填表单；submitEdit 校验后调 repository.updateCirclePost 更新 →
 *      弹「修改成功」→ 关闭页面。
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

    // 编辑模式：加载待编辑的动态，把正文与图片回填到界面。
    @Override
    public void loadPost(int postId) {
        Article post = repository.getArticleById(postId);
        if (post == null) {
            view.showToast("动态不存在或已删除");
            view.closePage();
            return;
        }
        // 农友圈动态无独立封面，图片都存在 coverUri；用 getContentImages 统一解析后拼成逗号串回填。
        StringBuilder sb = new StringBuilder();
        for (String uri : post.getContentImages()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(uri);
        }
        view.showExistingPost(post.content, sb.length() == 0 ? null : sb.toString());
    }

    // 编辑模式：校验内容非空 → 调 repository.updateCirclePost 更新 → 弹「修改成功」→ 关闭页面。
    @Override
    public void submitEdit(int postId, String content, String imageUris) {
        if (content == null || content.isEmpty()) {
            view.showToast("请输入动态内容");
            return;
        }
        repository.updateCirclePost(postId, content, imageUris);
        view.showToast("修改成功！");
        view.closePage();
    }
}
