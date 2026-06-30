package com.example.zhinongbao.mvp.sellerstore;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

public interface SellerStoreContract {
    interface View extends BaseView<Presenter> {
        void showStoreMeta(String seller, String storeName, String storePhone, String avatarUri, boolean ownStore);
        void showProducts(List<Product> products);
        void showStats(int totalOrders, double totalRevenue);
        // following=当前是否已关注；canFollow=是否显示关注按钮（非自己店铺且已登录）
        void showFollowState(boolean following, boolean canFollow);
        void showToast(String message);
    }

    interface Presenter extends BasePresenter {
        String getCurrentUser();
        void loadStore(String seller);
        void toggleFollow();
        void refreshProducts();
        void refreshStats();
        int getProductOrderCount(int productId);
        void delistProduct(int productId);
        void relistProduct(int productId);
        void updateStoreInfo(String name, String phone);
    }
}
