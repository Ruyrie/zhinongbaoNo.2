package com.example.zhinongbao.mvp.sellersales;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.repository.OrderRepository;

import java.util.List;

/**
 * ============================================================
 * 【卖家销售分析 / Seller Sales】Presenter（业务逻辑）
 * 整体逻辑：start() 按时间范围 scope(默认 all)拉取当前卖家的销售订单，累加每单
 *   净营收得到合计，回调 showSales。净营收 = 已付金额扣掉已生效退款。
 * 数据来源：走 OrderRepository（内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 = SellerSalesContract；View = SellerSalesAnalysisActivity；模型 = model/Order。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「销售分析」可看本组相关文件。
 * ============================================================
 */
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
            total += getOrderNetRevenue(order);   // 净额：退款生效后才扣减
        }
        view.showSales(scope, orders, total);
    }

    @Override
    public double getOrderPaidAmount(Order order) {
        return repository.getOrderPaidAmount(order);
    }

    @Override
    public double getOrderNetRevenue(Order order) {
        return repository.getOrderNetRevenue(order);
    }
}
