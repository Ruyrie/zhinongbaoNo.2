package com.example.zhinongbao.mvp.purchasemarket;

import android.content.Context;
import android.text.TextUtils;

import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.repository.PurchaseRepository;

public class PurchaseMarketPresenter implements PurchaseMarketContract.Presenter {
    private final PurchaseMarketContract.View view;
    private final PurchaseRepository repository;
    private final String currentUser;
    private final boolean sellerMode;

    public PurchaseMarketPresenter(Context context, PurchaseMarketContract.View view) {
        this.view = view;
        this.repository = new PurchaseRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.sellerMode = repository.isSellerMode();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showRequests(repository.getPurchaseRequests(), currentUser, sellerMode);
    }

    @Override
    public void onQuoteClick(PurchaseRequest request) {
        if (repository.hasQuoted(request.id, currentUser)) {
            view.showToast("您已报过价，查看报价列表");
            view.showQuoteList(request, repository.getQuotesForRequest(request.id));
        } else {
            view.showQuoteEditor(request);
        }
    }

    @Override
    public void onViewQuotesClick(PurchaseRequest request) {
        view.showQuoteList(request, repository.getQuotesForRequest(request.id));
    }

    @Override
    public void onItemClick(PurchaseRequest request) {
        if (sellerMode && !request.buyerUser.equals(currentUser)) {
            onQuoteClick(request);
        } else {
            onViewQuotesClick(request);
        }
    }

    @Override
    public void submitQuote(PurchaseRequest request, String priceStr, String desc) {
        if (TextUtils.isEmpty(priceStr)) {
            view.showToast("请填写报价金额");
            return;
        }
        try {
            double price = Double.parseDouble(priceStr);
            boolean ok = repository.addQuote(request.id, currentUser, price, desc);
            view.showToast(ok ? "报价成功！买家将看到您的报价" : "报价失败，请重试");
            if (ok) {
                refresh();
            }
        } catch (NumberFormatException e) {
            view.showToast("价格格式不正确");
        }
    }
}
