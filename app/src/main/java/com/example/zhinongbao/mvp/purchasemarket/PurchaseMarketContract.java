package com.example.zhinongbao.mvp.purchasemarket;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;

import java.util.List;

/**
 * ============================================================
 * 【采购市场 / PurchaseMarket】Contract（接口约定）
 * 约定内容：
 *   - View：显示需求列表、弹报价编辑框、弹报价列表、提示补收货地址、打开付款页、弹提示。
 *   - Presenter：刷新、点击报价/查看报价/点击条目、判断能否修改、删除需求、判断是否已报价、
 *     判断需求是否已成交锁定、提交报价、同意/拒绝报价。
 * 关键概念：sellerMode 表示当前用户是否具备报价资格；「锁定」指需求已被某报价付款成交，
 *   退款前不能再报价。
 * 配合的文件：View 实现 = PurchaseMarketActivity；Presenter 实现 = PurchaseMarketPresenter；
 *   数据访问 = repository/PurchaseRepository（经 ContentProvider 访问 SQLite）；
 *   模型 = model/PurchaseRequest、model/PurchaseQuote。
 * MVP 数据流位置：本文件是 View 与 Presenter 之间的「合同」。
 * 提示：在 IDE 里搜索「采购」可看本组相关文件。
 * ============================================================
 */
public interface PurchaseMarketContract {
    // View：Presenter 用这些方法更新采购市场界面
    interface View extends BaseView<Presenter> {
        void showRequests(List<PurchaseRequest> requests, String currentUser, boolean sellerMode); // 显示需求列表
        void showQuoteEditor(PurchaseRequest request);                        // 弹出报价编辑框
        void showQuoteList(PurchaseRequest request, List<PurchaseQuote> quotes); // 弹出报价列表
        void promptAddAddress();                                              // 无默认地址时提示去设置
        void openPayment(String orderId);                                     // 同意报价后打开订单付款页
        void showToast(String message);                                       // 弹提示
    }

    // Presenter：View（用户操作）用这些方法触发业务
    interface Presenter extends BasePresenter {
        void refresh();                                    // 刷新需求列表
        void onQuoteClick(PurchaseRequest request);        // 点击「报价」按钮
        void onViewQuotesClick(PurchaseRequest request);   // 点击「查看报价」按钮
        void onItemClick(PurchaseRequest request);         // 点击整条需求
        boolean canModify(PurchaseRequest request);        // 能否编辑/删除该需求
        boolean deleteRequest(PurchaseRequest request);    // 删除需求
        boolean hasQuoted(PurchaseRequest request);        // 当前用户是否已对该需求报过价
        boolean isRequestLocked(PurchaseRequest request);  // 该需求是否已成交锁定
        void submitQuote(PurchaseRequest request, String price, String desc, String images); // 提交/保存报价
        void acceptQuote(PurchaseRequest request, long quoteId, String reply); // 同意某报价（生成订单）
        void rejectQuote(PurchaseRequest request, long quoteId, String reply); // 拒绝某报价
    }
}
