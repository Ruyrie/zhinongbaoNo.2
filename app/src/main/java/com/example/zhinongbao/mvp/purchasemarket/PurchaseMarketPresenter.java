package com.example.zhinongbao.mvp.purchasemarket;

import android.content.Context;
import android.text.TextUtils;

import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.repository.PurchaseRepository;

/**
 * ============================================================
 * 【采购市场 / PurchaseMarket】Presenter（业务逻辑）
 * 整体逻辑：构造时创建 PurchaseRepository，取当前登录账号并判断是否可报价 canQuote；
 *   refresh 拉取全部需求交 View 渲染；onQuoteClick 分流（自己的需求转看报价、已成交则拦下、
 *   已报过价则展示列表、否则弹报价框）；submitQuote 做登录/自报/锁定/金额校验后写库；
 *   acceptQuote 校验「是本人需求」且有默认地址后生成订单并打开付款页；rejectQuote 拒绝报价。
 *   所有写操作成功后都 refresh 刷新界面。
 * 关键概念：isRequestLockedByPaidOrder 表示需求已被某报价付款成交（退款前锁定，不能再报价）；
 *   canModifyPurchaseRequest 表示需求未进入不可改状态（如已付款）才能编辑/删除。
 * 数据来源：repository/PurchaseRepository；Repository 内部经 ContentProvider 访问 SQLite。
 *   本类不直接碰数据库。
 * 配合的文件：接口约定 PurchaseMarketContract；View 实现 = PurchaseMarketActivity；
 *   适配器 adapter/PurchaseRequestAdapter；数据模型 model/PurchaseRequest、model/PurchaseQuote。
 * MVP 数据流位置：本类是 Presenter，居中协调 View 与 Repository。
 * 提示：在 IDE 里搜索「采购」可看本组相关文件。
 * ============================================================
 */
public class PurchaseMarketPresenter implements PurchaseMarketContract.Presenter {
    private final PurchaseMarketContract.View view;
    private final PurchaseRepository repository;
    private final String currentUser;
    private final boolean canQuote;

    public PurchaseMarketPresenter(Context context, PurchaseMarketContract.View view) {
        this.view = view;
        this.repository = new PurchaseRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.canQuote = currentUser != null && !currentUser.isEmpty();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showRequests(repository.getPurchaseRequests(), currentUser, canQuote);
    }

    @Override
    public boolean isRequestLocked(PurchaseRequest request) {
        return request != null && repository.isRequestLockedByPaidOrder(request.id);
    }

    @Override
    public void onQuoteClick(PurchaseRequest request) {
        if (currentUser == null || currentUser.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (request.buyerUser != null && request.buyerUser.equals(currentUser)) {
            onViewQuotesClick(request);
            return;
        }
        if (repository.isRequestLockedByPaidOrder(request.id)) {
            view.showToast("该需求已成交，退款后才能再次报价");
            return;
        }
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
        if (canQuote && request.buyerUser != null && !request.buyerUser.equals(currentUser)) {
            onQuoteClick(request);
        } else {
            onViewQuotesClick(request);
        }
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
            refresh();
        }
        return ok;
    }

    @Override
    public boolean hasQuoted(PurchaseRequest request) {
        return request != null && repository.hasQuoted(request.id, currentUser);
    }

    @Override
    public void submitQuote(PurchaseRequest request, String priceStr, String desc, String images) {
        if (currentUser == null || currentUser.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (request.buyerUser != null && request.buyerUser.equals(currentUser)) {
            view.showToast("不能给自己的采购需求报价");
            return;
        }
        if (repository.isRequestLockedByPaidOrder(request.id)) {
            view.showToast("该需求已成交，退款后才能再次报价");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            view.showToast("请填写报价金额");
            return;
        }
        try {
            double price = Double.parseDouble(priceStr.replace(",", "").replace("¥", "").trim());
            boolean ok = repository.addQuote(request.id, currentUser, price, desc, images);
            view.showToast(ok ? "报价成功！买家将看到您的报价" : "报价失败，请重试");
            if (ok) {
                refresh();
            }
        } catch (NumberFormatException e) {
            view.showToast("价格格式不正确");
        }
    }

    @Override
    public void acceptQuote(PurchaseRequest request, long quoteId, String reply) {
        if (!isOwnRequest(request)) {
            view.showToast("只能处理自己采购需求的报价");
            return;
        }
        if (!repository.hasDefaultAddress(currentUser)) {
            view.promptAddAddress();
            return;
        }
        String orderId = repository.acceptQuote(quoteId, reply);
        refresh();
        if (orderId != null) {
            view.openPayment(orderId);
        } else {
            view.showToast("操作失败，请重试");
        }
    }

    @Override
    public void rejectQuote(PurchaseRequest request, long quoteId, String reply) {
        if (!isOwnRequest(request)) {
            view.showToast("只能处理自己采购需求的报价");
            return;
        }
        boolean ok = repository.rejectQuote(quoteId, reply);
        view.showToast(ok ? "已拒绝报价" : "操作失败，请重试");
        refresh();
    }

    private boolean isOwnRequest(PurchaseRequest request) {
        return request != null && currentUser != null && currentUser.equals(request.buyerUser);
    }
}
