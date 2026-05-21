package com.example.zhinongbao.mvp.sellerorders;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.repository.OrderRepository;

import java.util.ArrayList;

public class SellerOrdersPresenter implements SellerOrdersContract.Presenter {
    private final SellerOrdersContract.View view;
    private final OrderRepository repository;
    private final String seller;
    private String currentFilter;
    private String salesScope;
    private String searchKeyword = "";

    public SellerOrdersPresenter(Context context, SellerOrdersContract.View view, String currentFilter, String salesScope) {
        this.view = view;
        this.repository = new OrderRepository(context.getApplicationContext());
        this.seller = repository.getLoggedUser();
        this.currentFilter = currentFilter == null ? "all" : currentFilter;
        this.salesScope = salesScope;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void setFilter(String filter) {
        this.currentFilter = filter;
        this.salesScope = null;
        refresh();
    }

    @Override
    public void setSearchKeyword(String keyword) {
        this.searchKeyword = keyword == null ? "" : keyword.trim();
    }

    @Override
    public void refresh() {
        if (seller == null) {
            view.showOrders(new ArrayList<>());
            return;
        }

        if (!searchKeyword.isEmpty()) {
            view.showOrders(repository.searchSellerSoldOrders(seller, searchKeyword));
        } else if (salesScope != null) {
            view.showOrders(repository.getSellerSalesOrders(seller, salesScope));
        } else if ("all".equals(currentFilter)) {
            view.showOrders(repository.getSellerSoldOrders(seller));
        } else {
            view.showOrders(repository.getSellerSoldOrdersByStatus(seller, currentFilter));
        }
    }

    @Override
    public void onShipClicked(Order order) {
        view.showShipDialog(order);
    }

    @Override
    public void onModifyPriceClicked(Order order) {
        view.showModifyPriceDialog(order);
    }

    @Override
    public void onRefundClicked(Order order) {
        view.showRefundDialog(order);
    }

    @Override
    public void onContactBuyer(Order order) {
        view.openChat(order.buyerUser, order.name);
    }

    @Override
    public void shipOrder(Order order, String shipType, String shipName, String shipNo, String shipPhone) {
        repository.shipOrder(order.orderId, shipType, shipName, shipNo, shipPhone);
        view.showToast("已发货");
        refresh();
    }

    @Override
    public void updateOrderPrice(Order order, double unitPrice, double discount) {
        if (repository.updateOrderPrice(order.orderId, unitPrice, discount)) {
            view.showToast("改价成功");
            refresh();
        }
    }

    @Override
    public void processRefund(Order order, double amount, String reason, boolean approve) {
        repository.processRefund(order.orderId, amount, reason, approve);
        view.showToast(approve ? "已同意退款" : "已拒绝退款，恢复为发货状态");
        refresh();
    }
}
