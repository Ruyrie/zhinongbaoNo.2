package com.example.zhinongbao.mvp.publish;

/**
 * ============================================================
 * 【发布入口 / Publish】Presenter（业务逻辑）
 * 整体逻辑：底部「发布」页的中转逻辑，只负责把两个按钮点击转成页面跳转 ——
 *   发布商品 → openAddProduct；发布采购需求 → openPurchaseMarket。无数据读写。
 * 数据来源：不涉及数据库。
 * 配合的文件：接口 = PublishContract；View = PublishFragment；跳转 AddProductActivity / 采购市场。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「发布」可看本组相关文件。
 * ============================================================
 */
public class PublishPresenter implements PublishContract.Presenter {
    private final PublishContract.View view;

    public PublishPresenter(PublishContract.View view) {
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void onPublishProductClicked() {
        view.openAddProduct();
    }

    @Override
    public void onPostPurchaseClicked() {
        view.openPurchaseMarket();
    }
}
