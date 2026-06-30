package com.example.zhinongbao.mvp.sellersales;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Order;

import java.util.List;

public interface SellerSalesContract {
    interface View extends BaseView<Presenter> {
        void showSales(String scope, List<Order> orders, double total);
    }

    interface Presenter extends BasePresenter {
        double getOrderPaidAmount(Order order);
        // 计入营收的净额（退款生效后才扣减；售后处理中不扣）
        double getOrderNetRevenue(Order order);
    }
}
