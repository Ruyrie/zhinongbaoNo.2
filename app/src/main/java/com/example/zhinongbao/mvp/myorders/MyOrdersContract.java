package com.example.zhinongbao.mvp.myorders;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Order;

import java.util.List;

public interface MyOrdersContract {
    interface View extends BaseView<Presenter> {
        void showOrders(List<Order> orders);

        void showCancelConfirm(Order order);

        void showRefundDialog(Order order);

        void showToast(String message);

        void openOrderDetail(String orderId);

        void openReview(int productId);
    }

    interface Presenter extends BasePresenter {
        void refresh();

        void onOrderClicked(Order order);

        void onPay(Order order);

        void onCancel(Order order);

        void onCancelConfirmed(Order order);

        void onReview(Order order);

        void onConfirmReceipt(Order order);

        void onRequestRefund(Order order);

        void onRefundConfirmed(Order order, String reason);
    }
}
