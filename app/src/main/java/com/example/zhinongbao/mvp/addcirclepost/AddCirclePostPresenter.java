package com.example.zhinongbao.mvp.addcirclepost;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;

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
