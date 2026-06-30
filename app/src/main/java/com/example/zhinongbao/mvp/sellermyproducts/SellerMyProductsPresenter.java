package com.example.zhinongbao.mvp.sellermyproducts;

import android.content.Context;

import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.repository.ProductRepository;

public class SellerMyProductsPresenter implements SellerMyProductsContract.Presenter {
    private final SellerMyProductsContract.View view;
    private final ProductRepository repository;
    private final String currentUser;

    public SellerMyProductsPresenter(Context context, SellerMyProductsContract.View view) {
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
        // 「我的货品」展示全部（含已下架），方便卖家管理与删除
        view.showProducts(repository.getAllProductsBySeller(currentUser));
    }

    @Override
    public void deleteProduct(Product product) {
        repository.deleteProduct(product.id);
        refresh();
    }

    @Override
    public int getProductOrderCount(int productId) {
        return repository.getProductOrderCount(productId);
    }

    @Override
    public double getProductSalesRevenue(int productId) {
        return repository.getProductSalesRevenue(productId);
    }
}
