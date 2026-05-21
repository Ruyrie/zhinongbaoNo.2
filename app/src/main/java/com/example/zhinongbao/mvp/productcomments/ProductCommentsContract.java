package com.example.zhinongbao.mvp.productcomments;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.ProductComment;

import java.util.List;

public interface ProductCommentsContract {
    interface View extends BaseView<Presenter> {
        void showComments(List<ProductComment> comments, String currentUser);
        void showToast(String message);
        void openAddComment(int productId);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        void writeReview();
        void deleteComment(ProductComment comment);
    }
}
