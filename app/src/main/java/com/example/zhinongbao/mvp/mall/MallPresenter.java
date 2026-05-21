package com.example.zhinongbao.mvp.mall;

import android.content.Context;

import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.repository.ProductRepository;

public class MallPresenter implements MallContract.Presenter {
    private final MallContract.View view;
    private final ProductRepository repository;
    private final String currentUser;

    public MallPresenter(Context context, MallContract.View view) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showProducts(repository.getProducts());
    }

    @Override
    public void addToCart(Product product) {
        if (product.seller != null && product.seller.equals(currentUser)) {
            view.showToast("不能购买自己发布的商品");
            return;
        }
        repository.addToCart(currentUser, product);
        view.showToast("已加入购物车");
    }
}
