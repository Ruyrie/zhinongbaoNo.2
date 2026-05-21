package com.example.zhinongbao.mvp.orderdetail;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.repository.OrderRepository;

public class OrderDetailPresenter implements OrderDetailContract.Presenter {
    private final OrderDetailContract.View view;
    private final OrderRepository repository;
    private final String orderId;
    private final String username;
    private Order order;

    public OrderDetailPresenter(Context context, OrderDetailContract.View view, String orderId) {
        this.view = view;
        this.orderId = orderId;
        this.repository = new OrderRepository(context.getApplicationContext());
        this.username = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        order = repository.getOrderById(orderId);
        if (order == null) {
            view.closePage();
            return;
        }

        if (Order.STATUS_PENDING.equals(order.status) && order.getRemainingMs() <= 0) {
            repository.updateOrderStatus(username, order.orderId, Order.STATUS_CANCELLED);
            order.status = Order.STATUS_CANCELLED;
        }

        boolean canComment = order.productId > 0 && !Order.ORDER_TYPE_PROCUREMENT.equals(order.orderType);
        view.showOrder(order, repository.getOrderPaidAmount(order), canComment);
    }

    @Override
    public void onPrimaryAction() {
        if (order == null) {
            return;
        }

        if (Order.STATUS_SHIPPED.equals(order.status)) {
            repository.confirmReceipt(username, order.orderId);
            view.showToast("已确认收货，现在可以评价商品");
            view.closePage();
        } else if (Order.STATUS_PAID.equals(order.status)) {
            view.showRefundDialog();
        } else if (Order.STATUS_PENDING.equals(order.status)) {
            repository.updateOrderStatus(username, order.orderId, Order.STATUS_PAID);
            view.showToast("支付成功！");
            view.closePage();
        }
    }

    @Override
    public void onSecondaryAction() {
        if (order == null) {
            return;
        }

        if (Order.STATUS_SHIPPED.equals(order.status)) {
            view.showRefundDialog();
        } else {
            view.showCancelConfirm();
        }
    }

    @Override
    public void confirmCancel() {
        if (order == null) {
            return;
        }

        repository.updateOrderStatus(username, order.orderId, Order.STATUS_CANCELLED);
        view.showToast("订单已取消");
        view.closePage();
    }

    @Override
    public void requestRefund(String reason) {
        if (order == null) {
            return;
        }

        String finalReason = reason == null || reason.trim().isEmpty() ? "买家申请退款" : reason.trim();
        repository.initiateRefund(order.orderId, finalReason);
        view.showToast("退款申请已提交");
        view.closePage();
    }
}
