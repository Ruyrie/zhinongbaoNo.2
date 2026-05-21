package com.example.zhinongbao.mvp.productfavorites;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

public interface ProductFavoritesContract {
    interface View extends BaseView<Presenter> {
        void showProducts(List<Product> products);
    }

    interface Presenter extends BasePresenter {
        void refresh();
    }
}
