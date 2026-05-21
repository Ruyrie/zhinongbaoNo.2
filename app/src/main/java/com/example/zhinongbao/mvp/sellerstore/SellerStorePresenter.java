package com.example.zhinongbao.mvp.sellerstore;

import android.content.Context;

import com.example.zhinongbao.repository.OrderRepository;
import com.example.zhinongbao.repository.ProductRepository;
import com.example.zhinongbao.repository.UserRepository;

public class SellerStorePresenter implements SellerStoreContract.Presenter {
    private final SellerStoreContract.View view;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final String currentUser;
    private String seller;

    public SellerStorePresenter(Context context, SellerStoreContract.View view) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.productRepository = new ProductRepository(appContext);
        this.userRepository = new UserRepository(appContext);
        this.orderRepository = new OrderRepository(appContext);
        this.currentUser = userRepository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public String getCurrentUser() {
        return currentUser;
    }

    @Override
    public void loadStore(String seller) {
        this.seller = seller == null || seller.isEmpty() ? currentUser : seller;
        productRepository.recordStoreView(currentUser, this.seller);
        boolean ownStore = this.seller != null && this.seller.equals(currentUser);
        view.showStoreMeta(this.seller, ownStore ? "我的店铺" : userRepository.getStoreName(this.seller),
                productRepository.getStorePhone(this.seller), ownStore);
        refreshProducts();
        if (ownStore) {
            refreshStats();
        }
    }

    @Override
    public void refreshProducts() {
        view.showProducts(productRepository.getProductsBySeller(seller));
    }

    @Override
    public void refreshStats() {
        view.showStats(orderRepository.getTotalOrderCountForSeller(seller), orderRepository.getTotalRevenueForSeller(seller));
    }

    @Override
    public int getProductOrderCount(int productId) {
        return productRepository.getProductOrderCount(productId);
    }

    @Override
    public void deleteProduct(int productId) {
        productRepository.deleteProduct(productId);
        refreshProducts();
        refreshStats();
    }

    @Override
    public void updateStoreInfo(String name, String phone) {
        userRepository.updateStoreInfo(seller, name, phone);
        loadStore(seller);
        view.showToast("店铺信息已更新");
    }
}
