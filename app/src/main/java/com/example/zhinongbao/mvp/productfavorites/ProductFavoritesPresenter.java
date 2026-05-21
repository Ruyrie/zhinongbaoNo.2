package com.example.zhinongbao.mvp.productfavorites;

import android.content.Context;

import com.example.zhinongbao.repository.ProductRepository;

public class ProductFavoritesPresenter implements ProductFavoritesContract.Presenter {
    private final ProductFavoritesContract.View view;
    private final ProductRepository repository;

    public ProductFavoritesPresenter(Context context, ProductFavoritesContract.View view) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showProducts(repository.getFavoriteProducts(repository.getLoggedUser()));
    }
}
