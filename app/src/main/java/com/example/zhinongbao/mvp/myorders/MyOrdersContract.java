package com.example.zhinongbao.mvp.myorders;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Order;

import java.util.List;

/**
 * ============================================================
 * 【我的订单 / My Orders】Contract（接口约定）
 * 约定内容：
 *   - View：刷新订单列表、弹「取消确认」框、弹「退款理由」框、弹提示、
 *     跳订单详情、跳评价页、跳与商家聊天。
 *   - Presenter：刷新、切换分类、单条点击、支付、取消/确认取消、去评价、
 *     确认收货、申请退款/确认退款。
 * 配合的文件：View 实现 = MyOrdersActivity；Presenter 实现 = MyOrdersPresenter；
 *   数据访问 = repository/OrderRepository；模型 = model/Order。
 * 在 MVP 数据流中的位置：接口层，是 View 与 Presenter 通信的契约。
 * 提示：在 IDE 里搜索「我的订单」可看本组相关文件。
 * ============================================================
 */
public interface MyOrdersContract {
    // View：Presenter 用这些方法更新订单界面
    interface View extends BaseView<Presenter> {
        void showOrders(List<Order> orders);      // 刷新订单列表

        void showCancelConfirm(Order order);      // 弹出取消订单确认框

        void showRefundDialog(Order order);       // 弹出填写退款理由的对话框

        void showToast(String message);           // 弹提示

        void openOrderDetail(String orderId);     // 跳转到订单详情页

        void openReview(int productId);           // 跳转到商品评价页

        void openStoreChat(Order order);          // 跳转到与商家聊天页
    }

    // Presenter：View（用户操作）用这些方法触发业务
    interface Presenter extends BasePresenter {
        void refresh();                           // 重新拉取并刷新订单

        void setFilter(String filter);            // 切换分类标签

        void onOrderClicked(Order order);         // 点击某条订单

        void onPay(Order order);                  // 支付

        void onCancel(Order order);               // 点「取消订单」（先确认）

        void onCancelConfirmed(Order order);      // 确认取消后真正执行

        void onReview(Order order);               // 去评价

        void onConfirmReceipt(Order order);       // 确认收货

        void onRequestRefund(Order order);        // 申请退款（可退则弹理由，不可退则联系商家）

        void onRefundConfirmed(Order order, String reason); // 填好理由后提交退款
    }
}
