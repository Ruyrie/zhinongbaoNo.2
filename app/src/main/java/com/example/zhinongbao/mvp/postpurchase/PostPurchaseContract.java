package com.example.zhinongbao.mvp.postpurchase;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.PurchaseRequest;

public interface PostPurchaseContract {
    interface View extends BaseView<Presenter> {
        void showExistingRequest(PurchaseRequest request);
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void loadRequest(long requestId);
        void submit(String name, String category, String qty, String unit, String targetPrice, String desc,
                String images);
        void submitEdit(long requestId, String name, String category, String qty, String unit,
                String targetPrice, String desc, String images);
    }
}
