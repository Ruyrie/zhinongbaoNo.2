package com.example.zhinongbao.mvp.purchasemarket;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;

import java.util.List;

public interface PurchaseMarketContract {
    interface View extends BaseView<Presenter> {
        void showRequests(List<PurchaseRequest> requests, String currentUser, boolean sellerMode);
        void showQuoteEditor(PurchaseRequest request);
        void showQuoteList(PurchaseRequest request, List<PurchaseQuote> quotes);
        void showToast(String message);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        void onQuoteClick(PurchaseRequest request);
        void onViewQuotesClick(PurchaseRequest request);
        void onItemClick(PurchaseRequest request);
        boolean canModify(PurchaseRequest request);
        boolean deleteRequest(PurchaseRequest request);
        boolean hasQuoted(PurchaseRequest request);
        void submitQuote(PurchaseRequest request, String price, String desc, String images);
    }
}
