package com.example.zhinongbao.mvp.publish;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface PublishContract {
    interface View extends BaseView<Presenter> {
        void openAddProduct();
        void openPurchaseMarket();
    }

    interface Presenter extends BasePresenter {
        void onPublishProductClicked();
        void onPostPurchaseClicked();
    }
}
