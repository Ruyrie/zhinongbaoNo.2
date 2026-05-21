package com.example.zhinongbao.mvp.sellerstore;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

public interface SellerStoreContract {
    interface View extends BaseView<Presenter> {
        void showStoreMeta(String seller, String storeName, String storePhone, boolean ownStore);
        void showProducts(List<Product> products);
        void showStats(int totalOrders, double totalRevenue);
        void showToast(String message);
    }

    interface Presenter extends BasePresenter {
        String getCurrentUser();
        void loadStore(String seller);
        void refreshProducts();
        void refreshStats();
        int getProductOrderCount(int productId);
        void deleteProduct(int productId);
        void updateStoreInfo(String name, String phone);
    }
}
