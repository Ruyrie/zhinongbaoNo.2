package com.example.zhinongbao.mvp.footprint;

import android.content.Context;

import com.example.zhinongbao.repository.ProductRepository;

public class FootprintPresenter implements FootprintContract.Presenter {
    private final FootprintContract.View view;
    private final ProductRepository repository;

    public FootprintPresenter(Context context, FootprintContract.View view) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        loadProducts();
    }

    @Override
    public void loadProducts() {
        view.showProductFootprints(repository.getProductFootprints(repository.getLoggedUser()));
    }

    @Override
    public void loadStores() {
        view.showStoreFootprints(repository.getStoreFootprints(repository.getLoggedUser()));
    }
}
