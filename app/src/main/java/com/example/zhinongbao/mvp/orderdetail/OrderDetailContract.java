package com.example.zhinongbao.mvp.orderdetail;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Order;

public interface OrderDetailContract {
    interface View extends BaseView<Presenter> {
        void showOrder(Order order, double paidAmount, boolean canComment, boolean canRequestRefund,
                String storeName, String storePhone, String storeAvatarUri);

        void showCancelConfirm();

        void showRefundDialog();

        void showToast(String message);

        void openStoreChat();

        void closePage();
    }

    interface Presenter extends BasePresenter {
        void onPrimaryAction();

        void onSecondaryAction();

        void confirmCancel();

        void requestRefund(String reason);

        void addToCart();
    }
}
