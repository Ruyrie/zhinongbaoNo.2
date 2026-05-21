package com.example.zhinongbao.mvp.productsearch;

import android.content.Context;

import com.example.zhinongbao.repository.ProductRepository;
import com.example.zhinongbao.repository.PurchaseRepository;

public class ProductSearchPresenter implements ProductSearchContract.Presenter {
    private final ProductSearchContract.View view;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;

    public ProductSearchPresenter(Context context, ProductSearchContract.View view) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.productRepository = new ProductRepository(appContext);
        this.purchaseRepository = new PurchaseRepository(appContext);
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.showInitialData(productRepository.getProducts(), purchaseRepository.getPurchaseRequests(),
                purchaseRepository.getLoggedUser(), purchaseRepository.isSellerMode());
    }

    @Override
    public void searchStores(String query) {
        view.showStoreResults(productRepository.searchStores(query));
    }

    @Override
    public java.util.List<com.example.zhinongbao.model.PurchaseRequest> getPurchaseRequests() {
        return purchaseRepository.getPurchaseRequests();
    }
}
