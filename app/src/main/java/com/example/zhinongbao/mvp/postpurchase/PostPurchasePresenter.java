package com.example.zhinongbao.mvp.postpurchase;

import android.content.Context;
import android.text.TextUtils;

import com.example.zhinongbao.repository.PurchaseRepository;

public class PostPurchasePresenter implements PostPurchaseContract.Presenter {
    private final PostPurchaseContract.View view;
    private final PurchaseRepository repository;

    public PostPurchasePresenter(Context context, PostPurchaseContract.View view) {
        this.view = view;
        this.repository = new PurchaseRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void submit(String name, String category, String qtyStr, String unit, String priceStr, String desc) {
        if (TextUtils.isEmpty(name)) {
            view.showToast("请填写货品名称");
            return;
        }
        if (TextUtils.isEmpty(qtyStr)) {
            view.showToast("请填写需求数量");
            return;
        }
        if (TextUtils.isEmpty(unit)) {
            view.showToast("请填写单位");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            view.showToast("请填写目标价格");
            return;
        }
        try {
            double quantity = Double.parseDouble(qtyStr);
            double targetPrice = Double.parseDouble(priceStr);
            boolean ok = repository.addPurchaseRequest(name, category, quantity, unit, targetPrice, desc);
            view.showToast(ok ? "采购需求发布成功！" : "发布失败，请重试");
            if (ok) {
                view.closePage();
            }
        } catch (NumberFormatException e) {
            view.showToast("数量或价格格式不正确");
        }
    }
}
