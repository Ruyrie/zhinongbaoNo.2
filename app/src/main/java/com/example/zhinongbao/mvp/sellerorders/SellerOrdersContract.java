package com.example.zhinongbao.mvp.sellerorders;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Order;

import java.util.List;

public interface SellerOrdersContract {
    interface View extends BaseView<Presenter> {
        void showOrders(List<Order> orders);

        void showToast(String message);

        void openChat(String buyerUser, String productName);

        void showShipDialog(Order order);

        void showModifyPriceDialog(Order order);

        void showRefundDialog(Order order);
    }

    interface Presenter extends BasePresenter {
        void setFilter(String filter);

        void setSearchKeyword(String keyword);

        void refresh();

        void onShipClicked(Order order);

        void onModifyPriceClicked(Order order);

        void onRefundClicked(Order order);

        void onContactBuyer(Order order);

        void shipOrder(Order order, String shipType, String shipName, String shipNo, String shipPhone);

        void updateOrderPrice(Order order, double unitPrice, double discount);

        void processRefund(Order order, double amount, String reason, boolean approve);
    }
}
