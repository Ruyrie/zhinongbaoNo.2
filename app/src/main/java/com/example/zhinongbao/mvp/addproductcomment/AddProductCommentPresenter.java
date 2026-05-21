package com.example.zhinongbao.mvp.addproductcomment;

import android.content.Context;

import com.example.zhinongbao.repository.ProductRepository;

public class AddProductCommentPresenter implements AddProductCommentContract.Presenter {
    private final AddProductCommentContract.View view;
    private final ProductRepository repository;
    private final int productId;
    private final String username;

    public AddProductCommentPresenter(Context context, AddProductCommentContract.View view, int productId) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.productId = productId;
        this.username = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public boolean canComment() {
        return repository.hasPurchasedProduct(username, productId) || "admin".equals(username);
    }

    @Override
    public void submit(String content, String images) {
        if ((content == null || content.isEmpty()) && (images == null || images.isEmpty())) {
            view.showToast("评价内容和图片不能同时为空");
            return;
        }
        repository.addProductComment(productId, username, content, images);
        view.showToast("评价发布成功");
        view.closePage();
    }
}
