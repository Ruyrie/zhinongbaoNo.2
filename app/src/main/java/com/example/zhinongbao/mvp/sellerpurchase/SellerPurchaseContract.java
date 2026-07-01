package com.example.zhinongbao.mvp.sellerpurchase;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;

import java.util.List;

/**
 * ============================================================
 * 【卖家采购管理 / Seller Purchase】Contract（接口约定）
 * 约定内容：View（展示采购大厅/我的报价/我的需求三个 Tab、弹提示、提示加地址、跳支付）；
 *   Presenter（切 Tab、提交报价、接受/拒绝报价、查报价、判断是否已报价/锁定/可改、删需求）。
 * 关键概念：卖家可对买家发布的采购需求(PurchaseRequest)进行报价(PurchaseQuote)；
 *   买家接受报价即成交生成订单，成交后需求会被锁定。
 * 配合的文件：View 实现 = SellerPurchaseMgmtActivity；Presenter 实现 = SellerPurchasePresenter；
 *   数据访问 = repository/PurchaseRepository；模型 = model/PurchaseRequest、model/PurchaseQuote。
 * 提示：在 IDE 里搜索「采购管理」可看本组相关文件。
 * ============================================================
 */
public interface SellerPurchaseContract {
    interface View extends BaseView<Presenter> {
        void showMarket(List<PurchaseRequest> requests);
        void showMyQuotes(List<PurchaseQuote> quotes);
        void showMyRequests(List<PurchaseRequest> requests);
        void showToast(String message);
        void promptAddAddress();
        void openPayment(String orderId);
    }

    interface Presenter extends BasePresenter {
        void switchTab(int tab);
        void submitQuote(PurchaseRequest request, String price, String desc, String images);
        void acceptQuote(long quoteId, String reply);
        void rejectQuote(long quoteId, String reply);
        List<PurchaseQuote> getQuotesForRequest(long requestId);
        boolean hasQuoted(long requestId);
        boolean isRequestLocked(long requestId);
        boolean canModify(PurchaseRequest request);
        boolean deleteRequest(PurchaseRequest request);
    }
}
