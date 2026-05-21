package com.example.zhinongbao.mvp.mall;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

public interface MallContract {
    interface View extends BaseView<Presenter> {
        void showProducts(List<Product> products);
        void showToast(String message);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        void addToCart(Product product);
    }
}
