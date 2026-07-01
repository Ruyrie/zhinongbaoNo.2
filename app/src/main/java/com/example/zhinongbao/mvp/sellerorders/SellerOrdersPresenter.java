package com.example.zhinongbao.mvp.sellerorders;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.repository.OrderRepository;
import com.example.zhinongbao.repository.ProductRepository;

import java.util.ArrayList;

/**
 * ============================================================
 * 【卖家订单 / Seller Orders】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1. 构造时创建 OrderRepository/ProductRepository，取当前登录卖家用户名与初始过滤/销售范围。
 *   2. refresh()：按优先级选择数据源——有搜索词→搜索；有销售范围→按范围；否则按状态过滤或全部。
 *   3. 三种操作按钮点击只负责让 View 弹对应对话框，真正提交由 shipOrder/updateOrderPrice/processRefund 完成。
 *   4. onContactBuyer 打开与买家的聊天。
 * 数据来源：OrderRepository（订单）与 ProductRepository（商品），内部经 ContentProvider 访问 SQLite；本类不直接碰数据库。
 * 配合的文件：接口 = SellerOrdersContract；View = activity/SellerOrdersActivity；模型 = model/Order、model/Product。
 * 在 MVP 数据流中的位置：View 与 Repository 之间的业务中枢。
 * 提示：在 IDE 里搜索「卖家订单」可看本组相关文件。
 * ============================================================
 */
public class SellerOrdersPresenter implements SellerOrdersContract.Presenter {
    private final SellerOrdersContract.View view;
    private final OrderRepository repository;
    private final ProductRepository productRepository;
    private final String seller;
    private String currentFilter;
    private String salesScope;
    private String searchKeyword = "";

    public SellerOrdersPresenter(Context context, SellerOrdersContract.View view, String currentFilter, String salesScope) {
        this.view = view;
        this.repository = new OrderRepository(context.getApplicationContext());
        this.productRepository = new ProductRepository(context.getApplicationContext());
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
        // 按优先级选择数据源：搜索词 > 销售范围 > 全部/按状态
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
        String type = shipType == null ? "express" : shipType;
        String name = shipName == null ? "" : shipName.trim();
        String no = shipNo == null ? "" : shipNo.trim();
        String phone = shipPhone == null ? "" : shipPhone.trim();
        if (name.isEmpty() || no.isEmpty() || ("custom".equals(type) && phone.isEmpty())) {
            view.showToast("请填写完整发货信息");
            return;
        }
        if (repository.shipOrder(order.orderId, type, name, no, phone)) {
            view.showToast("已发货");
            refresh();
        }
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
        view.showToast(approve ? "已同意退款" : "已拒绝退款申请");
        refresh();
    }

    @Override
    public Product getProductById(int productId) {
        return productRepository.getProductById(productId);
    }
}
