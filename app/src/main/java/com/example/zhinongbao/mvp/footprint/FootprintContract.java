package com.example.zhinongbao.mvp.footprint;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.StoreFootprint;

import java.util.List;

public interface FootprintContract {
    interface View extends BaseView<Presenter> {
        void showProductFootprints(List<Product> products);
        void showStoreFootprints(List<StoreFootprint> stores);
    }

    interface Presenter extends BasePresenter {
        void loadProducts();
        void loadStores();
    }
}
