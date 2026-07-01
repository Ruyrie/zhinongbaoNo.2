package com.example.zhinongbao.mvp.orderdetail;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Order;

/**
 * ============================================================
 * 【订单详情 / Order Detail】Contract（接口约定）
 * 约定内容：
 *   - View：渲染订单详情（含实付金额、能否评价、能否退款、店铺名/电话/头像）、
 *     弹取消确认、弹退款理由、弹提示、跳与商家聊天、关闭页面。
 *   - Presenter：处理主按钮、次按钮点击、确认取消、提交退款、加入购物车。
 * 关键概念：主/次按钮的实际含义随订单状态变化，因此用 onPrimaryAction /
 *   onSecondaryAction 统一入口，具体行为由 Presenter 依状态判断。
 * 配合的文件：View 实现 = OrderDetailActivity；Presenter 实现 = OrderDetailPresenter；
 *   数据访问 = repository/OrderRepository 等；模型 = model/Order。
 * 在 MVP 数据流中的位置：接口层，是 View 与 Presenter 通信的契约。
 * 提示：在 IDE 里搜索「订单详情」可看本组相关文件。
 * ============================================================
 */
public interface OrderDetailContract {
    // View：Presenter 用这些方法更新订单详情界面
    interface View extends BaseView<Presenter> {
        // 渲染订单详情：实付金额、能否评价、能否退款、店铺名/电话/头像
        void showOrder(Order order, double paidAmount, boolean canComment, boolean canRequestRefund,
                String storeName, String storePhone, String storeAvatarUri);

        void showCancelConfirm();   // 弹出取消订单确认框

        void showRefundDialog();    // 弹出填写退款理由的对话框

        void showToast(String message); // 弹提示

        void openStoreChat();       // 跳转到与商家聊天页

        void closePage();           // 关闭当前页
    }

    // Presenter：View（用户操作）用这些方法触发业务
    interface Presenter extends BasePresenter {
        void onPrimaryAction();     // 底部主按钮点击（含义随状态变化）

        void onSecondaryAction();   // 底部次按钮点击（含义随状态变化）

        void confirmCancel();       // 确认取消订单

        void requestRefund(String reason); // 提交退款申请

        void addToCart();           // 把该商品加入购物车
    }
}
