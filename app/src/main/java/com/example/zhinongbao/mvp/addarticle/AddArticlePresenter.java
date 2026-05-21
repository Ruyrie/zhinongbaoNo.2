package com.example.zhinongbao.mvp.addarticle;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

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
