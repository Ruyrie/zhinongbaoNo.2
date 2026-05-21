package com.example.zhinongbao.mvp.myorders;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

public class MyOrdersPresenter implements MyOrdersContract.Presenter {
    private final MyOrdersContract.View view;
    private final OrderRepository repository;
    private final String username;
    private final String filter;

    public MyOrdersPresenter(Context context, MyOrdersContract.View view, String filter) {
        this.view = view;
        this.repository = new OrderRepository(context.getApplicationContext());
        this.username = repository.getLoggedUser();
        this.filter = filter;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        if (username == null) {
            view.showOrders(new ArrayList<>());
            return;
        }
        view.showOrders(loadOrders());
    }

    @Override
    public void onOrderClicked(Order order) {
        view.openOrderDetail(order.orderId);
    }

    @Override
    public void onPay(Order order) {
        repository.updateOrderStatus(username, order.orderId, Order.STATUS_PAID);
        view.showToast("支付成功！");
        refresh();
    }

    @Override
    public void onCancel(Order order) {
        view.showCancelConfirm(order);
    }

    @Override
    public void onCancelConfirmed(Order order) {
        repository.updateOrderStatus(username, order.orderId, Order.STATUS_CANCELLED);
        refresh();
    }

    @Override
    public void onReview(Order order) {
        view.openReview(order.productId);
    }

    @Override
    public void onConfirmReceipt(Order order) {
        repository.confirmReceipt(username, order.orderId);
        view.showToast("已确认收货，现在可以评价商品");
        refresh();
    }

    @Override
    public void onRequestRefund(Order order) {
        view.showRefundDialog(order);
    }

    @Override
    public void onRefundConfirmed(Order order, String reason) {
        String finalReason = reason == null || reason.trim().isEmpty() ? "买家申请退款" : reason.trim();
        repository.initiateRefund(order.orderId, finalReason);
        view.showToast("退款申请已提交");
        refresh();
    }

    private List<Order> loadOrders() {
        if ("reviewing".equals(filter)) {
            return repository.getPendingReviewOrders(username);
        }

        List<Order> result = new ArrayList<>();
        for (Order order : repository.getOrders(username)) {
            if (filter == null || filter.isEmpty()) {
                result.add(order);
            } else if ("shipping".equals(filter) && Order.STATUS_PAID.equals(order.status)) {
                result.add(order);
            } else if ("receiving".equals(filter) && Order.STATUS_SHIPPED.equals(order.status)) {
                result.add(order);
            } else if ("pending".equals(filter) && Order.STATUS_PENDING.equals(order.status)) {
                result.add(order);
            } else if ("refund".equals(filter) && Order.STATUS_REFUND.equals(order.status)) {
                result.add(order);
            }
        }
        return result;
    }
}
