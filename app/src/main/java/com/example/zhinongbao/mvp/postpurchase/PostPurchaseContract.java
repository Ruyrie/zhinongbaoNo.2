package com.example.zhinongbao.mvp.postpurchase;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface PostPurchaseContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void submit(String name, String category, String qty, String unit, String targetPrice, String desc);
    }
}
