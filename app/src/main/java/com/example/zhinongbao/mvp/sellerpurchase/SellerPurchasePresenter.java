package com.example.zhinongbao.mvp.sellerpurchase;

import android.content.Context;

import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.repository.PurchaseRepository;

import java.util.List;

/**
 * ============================================================
 * 【卖家采购管理 / Seller Purchase】Presenter（业务逻辑）
 * 整体逻辑：switchTab() 按 Tab(采购大厅/我的报价/我的需求)拉取对应数据；
 *   submitQuote() 校验价格并报价(成交锁定的需求禁止再报)；acceptQuote() 前
 *   先检查默认收货地址，接受后生成订单并跳支付；另含拒绝报价、删需求等。
 * 数据来源：走 PurchaseRepository（内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 = SellerPurchaseContract；View = SellerPurchaseMgmtActivity。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「采购管理」可看本组相关文件。
 * ============================================================
 */
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
            view.showMarket(repository.getPurchaseRequests());
        } else if (tab == 1) {
            view.showMyQuotes(repository.getQuotesBySellerUser(currentUser));
        } else {
            view.showMyRequests(repository.getMyPurchaseRequests(currentUser));
        }
    }

    @Override
    public void submitQuote(PurchaseRequest request, String price, String desc, String images) {
        if (repository.isRequestLockedByPaidOrder(request.id)) {
            view.showToast("该需求已成交，退款后才能再次报价");
            return;
        }
        try {
            boolean ok = repository.addQuote(request.id, currentUser,
                    Double.parseDouble(price.replace(",", "").replace("¥", "").trim()), desc, images);
            view.showToast(ok ? "报价成功" : "报价失败");
            switchTab(currentTab);
        } catch (Exception e) {
            view.showToast("价格格式不正确");
        }
    }

    @Override
    public void acceptQuote(long quoteId, String reply) {
        if (!repository.hasDefaultAddress(currentUser)) {
            view.promptAddAddress();
            return;
        }
        String orderId = repository.acceptQuote(quoteId, reply);
        switchTab(currentTab);
        if (orderId != null) {
            view.openPayment(orderId);
        } else {
            view.showToast("操作失败，请重试");
        }
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

    @Override
    public boolean hasQuoted(long requestId) {
        return repository.hasQuoted(requestId, currentUser);
    }

    @Override
    public boolean isRequestLocked(long requestId) {
        return repository.isRequestLockedByPaidOrder(requestId);
    }

    @Override
    public boolean canModify(PurchaseRequest request) {
        return request != null && repository.canModifyPurchaseRequest(request.id, currentUser);
    }

    @Override
    public boolean deleteRequest(PurchaseRequest request) {
        if (request == null) {
            return false;
        }
        boolean ok = repository.deletePurchaseRequest(request.id);
        view.showToast(ok ? "采购需求已删除" : "删除失败，请确认订单未付款");
        if (ok) {
            switchTab(currentTab);
        }
        return ok;
    }
}
