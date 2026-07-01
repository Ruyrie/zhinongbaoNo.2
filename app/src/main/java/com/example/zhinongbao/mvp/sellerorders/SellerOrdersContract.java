package com.example.zhinongbao.mvp.sellerorders;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.Product;

import java.util.List;

/**
 * ============================================================
 * 【卖家订单 / Seller Orders】Contract（接口约定）
 * 约定内容：
 *   - View：展示订单列表、弹提示、打开聊天、弹发货/改价/退款三种对话框。
 *   - Presenter：切换状态过滤/搜索、刷新，处理发货、改价、退款、联系买家等业务。
 * 关键概念：订单按状态过滤（all/待付款/待发货/已发货/待处理售后）；还支持关键词搜索
 *   与「按销售范围(今日/本月/全部)」查看销售订单。
 * 数据来源：Presenter 走 OrderRepository（订单）与 ProductRepository（商品），
 *   内部经 ContentProvider 访问 SQLite；本接口不碰数据库。
 * 配合的文件：View 实现 = activity/SellerOrdersActivity；Presenter 实现 = SellerOrdersPresenter；
 *   适配器 = adapter/SellerOrderAdapter；模型 = model/Order、model/Product。
 * 在 MVP 数据流中的位置：View 与 Presenter 之间的契约层。
 * 提示：在 IDE 里搜索「卖家订单」可看本组相关文件。
 * ============================================================
 */
public interface SellerOrdersContract {
    interface View extends BaseView<Presenter> {
        void showOrders(List<Order> orders);                    // 展示订单列表

        void showToast(String message);                         // 弹提示

        void openChat(String buyerUser, String productName);    // 打开与买家的聊天

        void showShipDialog(Order order);                       // 弹发货对话框

        void showModifyPriceDialog(Order order);                // 弹改价对话框

        void showRefundDialog(Order order);                     // 弹处理售后（退款）对话框
    }

    interface Presenter extends BasePresenter {
        void setFilter(String filter);          // 设置状态过滤并刷新

        void setSearchKeyword(String keyword);  // 设置搜索关键词

        void refresh();                         // 按当前过滤/搜索/范围重新加载列表

        void onShipClicked(Order order);        // 点「去发货」

        void onModifyPriceClicked(Order order); // 点「修改价格」

        void onRefundClicked(Order order);      // 点「处理售后」

        void onContactBuyer(Order order);       // 点「联系买家」

        void shipOrder(Order order, String shipType, String shipName, String shipNo, String shipPhone); // 提交发货信息

        void updateOrderPrice(Order order, double unitPrice, double discount); // 提交改价

        void processRefund(Order order, double amount, String reason, boolean approve); // 处理退款（同意/拒绝）

        Product getProductById(int productId);  // 按 id 取商品（用于展示封面/详情）
    }
}
