package com.example.zhinongbao.mvp.addproduct;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

public interface AddProductContract {
    interface View extends BaseView<Presenter> {
        void showStorePhone(String phone);
        void showExistingProduct(Product product);
        void showToast(String message, boolean longToast);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void loadProduct(int productId);
        void submitProduct(int productId, String name, String desc, double price, String coverUri, String categories,
                String storePhone, String brand, String origin, String spec, String packageType);
    }
}
