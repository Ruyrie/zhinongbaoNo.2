package com.example.zhinongbao.mvp.productdetail;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.ProductComment;

public interface ProductDetailContract {
    interface View extends BaseView<Presenter> {
        void showProduct(Product product, String username, String seller, boolean ownProduct, boolean favorited);

        void showCommentPreview(int count, ProductComment latest);

        void showToast(String message);

        void setFavoriteState(boolean favorited);

        void openAddressManager();

        void openMyOrders();

        void closePage();
    }

    interface Presenter extends BasePresenter {
        void refreshComments();

        void addToCart();

        void buyNow();

        void toggleFavorite();

        void delistProduct();

        void relistProduct();

        String getStorePhone();
    }
}
