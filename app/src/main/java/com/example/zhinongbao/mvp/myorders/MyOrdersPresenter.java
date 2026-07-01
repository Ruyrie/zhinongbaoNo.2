package com.example.zhinongbao.mvp.myorders;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【我的订单 / My Orders】Presenter（业务逻辑）
 * 整体逻辑：构造时拿到当前登录用户名并创建 OrderRepository；refresh 时按 filter
 *   从 Repository 拉对应状态的订单交给 View；各操作方法先调 Repository 改数据库
 *   状态，再 refresh 或提示；退款前先用 canRequestRefund 判断是否还在时限内。
 * 数据来源：走 repository/OrderRepository，Repository 内部通过 ContentProvider
 *   访问 SQLite；本类不直接操作数据库。
 * 配合的文件：接口约定 mvp/myorders/MyOrdersContract；View 实现 = MyOrdersActivity；
 *   数据访问 = repository/OrderRepository；模型 = model/Order。
 * 在 MVP 数据流中的位置：中间的业务层，连接 View 与 Repository。
 * 提示：在 IDE 里搜索「我的订单」可看本组相关文件。
 * ============================================================
 */
public class MyOrdersPresenter implements MyOrdersContract.Presenter {
    private final MyOrdersContract.View view;      // 对应的界面
    private final OrderRepository repository;      // 订单数据访问入口
    private final String username;                 // 当前登录用户
    private String filter;                          // 当前分类过滤条件

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
    public void setFilter(String filter) {
        this.filter = filter;
        refresh();
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
        // 采购订单不支持评价，提示语不带「评价」
        boolean reviewable = order.productId > 0 && !Order.ORDER_TYPE_PROCUREMENT.equals(order.orderType);
        view.showToast(reviewable ? "已确认收货，现在可以评价商品" : "已确认收货");
        refresh();
    }

    @Override
    public void onRequestRefund(Order order) {
        if (repository.canRequestRefund(order)) {
            view.showRefundDialog(order);
        } else {
            view.openStoreChat(order);
        }
    }

    @Override
    public void onRefundConfirmed(Order order, String reason) {
        String finalReason = reason == null || reason.trim().isEmpty() ? "买家申请退款" : reason.trim();
        if (repository.initiateRefund(order.orderId, finalReason)) {
            view.showToast("退款申请已提交");
        } else {
            view.showToast("当前订单已超过退款时限，请联系客服");
        }
        refresh();
    }

    // 按当前分类从 Repository 取订单：待评价单独查，其余在全部订单里按状态筛选
    private List<Order> loadOrders() {
        if ("reviewing".equals(filter)) {
            return repository.getPendingReviewOrders(username);
        }

        List<Order> result = new ArrayList<>();
        for (Order order : repository.getOrders(username)) {
            if (filter == null || filter.isEmpty() || "all".equals(filter)) {
                result.add(order);
            } else if ("shipping".equals(filter) && Order.STATUS_PAID.equals(order.status)) {
                result.add(order);
            } else if ("receiving".equals(filter) && Order.STATUS_SHIPPED.equals(order.status)) {
                result.add(order);
            } else if ("pending".equals(filter) && Order.STATUS_PENDING.equals(order.status)) {
                result.add(order);
            } else if ("refund".equals(filter) && Order.STATUS_REFUND.equals(order.status)) {
                result.add(order);
            } else if ("refundable".equals(filter) && repository.canRequestRefund(order)) {
                result.add(order);
            }
        }
        return result;
    }
}
