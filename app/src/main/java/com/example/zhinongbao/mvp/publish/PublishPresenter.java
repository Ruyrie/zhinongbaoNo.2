package com.example.zhinongbao.mvp.publish;

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
