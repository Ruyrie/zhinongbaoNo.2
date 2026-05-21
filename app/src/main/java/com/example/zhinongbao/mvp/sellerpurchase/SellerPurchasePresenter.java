package com.example.zhinongbao.mvp.sellerpurchase;

import android.content.Context;

import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.repository.PurchaseRepository;

import java.util.List;

public class SellerPurchasePresenter implements SellerPurchaseContract.Presenter {
    private final SellerPurchaseContract.View view;
    private final PurchaseRepository repository;
    private final String currentUser;
    private int currentTab;

    public SellerPurchasePresenter(Context context, SellerPurchaseContract.View view) {
        this.view = view;
        this.repository = new PurchaseRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        switchTab(0);
    }

    @Override
    public void switchTab(int tab) {
        currentTab = tab;
        if (tab == 0) {
            view.showMarket(repository.getMarketRequestsForSeller(currentUser));
        } else if (tab == 1) {
            view.showMyQuotes(repository.getQuotesBySellerUser(currentUser));
        } else {
            view.showMyRequests(repository.getMyPurchaseRequests(currentUser));
        }
    }

    @Override
    public void submitQuote(PurchaseRequest request, String price, String desc) {
        try {
            boolean ok = repository.addQuote(request.id, currentUser, Double.parseDouble(price), desc);
            view.showToast(ok ? "报价成功" : "报价失败");
            switchTab(currentTab);
        } catch (Exception e) {
            view.showToast("价格格式不正确");
        }
    }

    @Override
    public void acceptQuote(long quoteId, String reply) {
        if (!repository.hasDefaultAddress(currentUser)) {
            view.showToast("请先在设置中添加收货地址");
            return;
        }
        repository.acceptQuote(quoteId, reply);
        view.showToast("已接受报价，请到我的订单完成支付");
        switchTab(currentTab);
    }

    @Override
    public void rejectQuote(long quoteId, String reply) {
        repository.rejectQuote(quoteId, reply);
        view.showToast("已拒绝报价");
        switchTab(currentTab);
    }

    @Override
    public List<PurchaseQuote> getQuotesForRequest(long requestId) {
        return repository.getQuotesForRequestWithStatus(requestId);
    }
}
