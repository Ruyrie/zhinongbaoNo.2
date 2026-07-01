package com.example.zhinongbao.mvp.sellersales;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Order;

import java.util.List;

/**
 * ============================================================
 * 【卖家销售分析 / Seller Sales】Contract（接口约定）
 * 约定内容：View（按时间范围展示订单列表与营收合计）；Presenter（取单笔已付金额、
 *   取单笔净营收——退款生效后才扣减，售后处理中不扣）。
 * 配合的文件：View 实现 = SellerSalesAnalysisActivity；Presenter 实现 = SellerSalesPresenter；
 *   数据访问 = repository/OrderRepository；模型 = model/Order。
 * 提示：在 IDE 里搜索「销售分析」可看本组相关文件。
 * ============================================================
 */
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
