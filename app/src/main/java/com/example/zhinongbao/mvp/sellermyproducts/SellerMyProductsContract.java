package com.example.zhinongbao.mvp.sellermyproducts;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

public interface SellerMyProductsContract {
    interface View extends BaseView<Presenter> {
        void showProducts(List<Product> products);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        void deleteProduct(Product product);
        int getProductOrderCount(int productId);
        double getProductSalesRevenue(int productId);
    }
}
