package com.example.zhinongbao.mvp.productcomments;

import android.content.Context;

import com.example.zhinongbao.model.ProductComment;
import com.example.zhinongbao.repository.ProductRepository;

public class ProductCommentsPresenter implements ProductCommentsContract.Presenter {
    private final ProductCommentsContract.View view;
    private final ProductRepository repository;
    private final int productId;
    private final String currentUser;

    public ProductCommentsPresenter(Context context, ProductCommentsContract.View view, int productId) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.productId = productId;
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showComments(repository.getProductComments(productId), currentUser);
    }

    @Override
    public void writeReview() {
        if (repository.hasPurchasedProduct(currentUser, productId) || "admin".equals(currentUser)) {
            view.openAddComment(productId);
        } else {
            view.showToast("购买该商品后才可以进行评价哦");
        }
    }

    @Override
    public void deleteComment(ProductComment comment) {
        repository.deleteProductComment(comment.id);
        refresh();
    }
}
