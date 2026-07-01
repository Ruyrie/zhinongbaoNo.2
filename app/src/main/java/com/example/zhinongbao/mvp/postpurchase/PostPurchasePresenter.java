package com.example.zhinongbao.mvp.postpurchase;

import android.content.Context;
import android.text.TextUtils;

import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.repository.PurchaseRepository;

/**
 * ============================================================
 * 【发布采购需求 / PostPurchase】Presenter（业务逻辑）
 * 整体逻辑：构造时创建 PurchaseRepository 并注册给 View；loadRequest 取待编辑需求，
 *   不可修改则提示并关页，否则回填表单；submit/submitEdit 都走 submitInternal，
 *   依次校验名称/数量/单位/单价非空、数字格式正确，再按有无 requestId 调 Repository
 *   的更新或新增方法，成功则关闭页面。
 * 数据来源：repository/PurchaseRepository；Repository 内部经 ContentProvider 访问 SQLite。
 *   本类不直接碰数据库。
 * 配合的文件：接口约定 PostPurchaseContract；View 实现 = PostPurchaseActivity；
 *   数据模型 model/PurchaseRequest。
 * MVP 数据流位置：本类是 Presenter，居中协调 View 与 Repository。
 * 提示：在 IDE 里搜索「采购」可看本组相关文件。
 * ============================================================
 */
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
    public void loadRequest(long requestId) {
        PurchaseRequest request = repository.getPurchaseRequestById(requestId);
        if (request == null || !repository.canModifyPurchaseRequest(requestId, repository.getLoggedUser())) {
            view.showToast("该采购需求当前不可修改");
            view.closePage();
            return;
        }
        view.showExistingRequest(request);
    }

    @Override
    public void submit(String name, String category, String qtyStr, String unit, String priceStr, String desc,
            String images) {
        submitInternal(-1, name, category, qtyStr, unit, priceStr, desc, images);
    }

    @Override
    public void submitEdit(long requestId, String name, String category, String qtyStr, String unit,
            String priceStr, String desc, String images) {
        submitInternal(requestId, name, category, qtyStr, unit, priceStr, desc, images);
    }

    private void submitInternal(long requestId, String name, String category, String qtyStr, String unit,
            String priceStr, String desc, String images) {
        if (TextUtils.isEmpty(name)) {
            view.showToast("请填写货品名称");
            return;
        }
        if (TextUtils.isEmpty(qtyStr)) {
            view.showToast("请填写购买数量");
            return;
        }
        if (TextUtils.isEmpty(unit)) {
            view.showToast("请填写单位");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            view.showToast("请填写预算单价");
            return;
        }
        try {
            double quantity = Double.parseDouble(qtyStr);
            double targetPrice = Double.parseDouble(priceStr);
            boolean ok = requestId > 0
                    ? repository.updatePurchaseRequest(requestId, name, category, quantity, unit, targetPrice, desc,
                            images)
                    : repository.addPurchaseRequest(name, category, quantity, unit, targetPrice, desc, images);
            view.showToast(ok ? (requestId > 0 ? "采购需求已更新" : "采购需求发布成功！")
                    : (requestId > 0 ? "更新失败，请确认订单未付款" : "发布失败，请重试"));
            if (ok) {
                view.closePage();
            }
        } catch (NumberFormatException e) {
            view.showToast("数量或价格格式不正确");
        }
    }
}
