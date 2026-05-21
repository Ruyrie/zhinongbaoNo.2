package com.example.zhinongbao.mvp.sellersales;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.repository.OrderRepository;

import java.util.List;

public class SellerSalesPresenter implements SellerSalesContract.Presenter {
    private final SellerSalesContract.View view;
    private final OrderRepository repository;
    private final String scope;

    public SellerSalesPresenter(Context context, SellerSalesContract.View view, String scope) {
        this.view = view;
        this.repository = new OrderRepository(context.getApplicationContext());
        this.scope = scope == null || scope.isEmpty() ? "all" : scope;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        List<Order> orders = repository.getSellerSalesOrders(repository.getLoggedUser(), scope);
        double total = 0;
        for (Order order : orders) {
            total += Math.max(0, getOrderPaidAmount(order) - order.refundAmount);
        }
        view.showSales(scope, orders, total);
    }

    @Override
    public double getOrderPaidAmount(Order order) {
        return repository.getOrderPaidAmount(order);
    }
}
