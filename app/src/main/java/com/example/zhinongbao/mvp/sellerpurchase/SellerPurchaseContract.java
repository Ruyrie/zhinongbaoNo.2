package com.example.zhinongbao.mvp.sellerpurchase;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;

import java.util.List;

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
