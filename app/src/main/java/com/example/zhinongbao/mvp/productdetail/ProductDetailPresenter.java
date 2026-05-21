package com.example.zhinongbao.mvp.productdetail;

import android.content.Context;

import com.example.zhinongbao.ChatActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.ProductComment;
import com.example.zhinongbao.repository.ProductRepository;

import java.util.List;

public class ProductDetailPresenter implements ProductDetailContract.Presenter {
    private final ProductDetailContract.View view;
    private final ProductRepository repository;
    private final int productId;
    private Product product;
    private String username;
    private String seller;

    public ProductDetailPresenter(Context context, ProductDetailContract.View view, int productId) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.productId = productId;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        product = repository.getProductById(productId);
        if (product == null) {
            view.closePage();
            return;
        }
        username = repository.getLoggedUser();
        repository.recordProductView(username, product.id);
        product = repository.getProductById(productId);
        seller = product.seller != null && !product.seller.isEmpty() ? product.seller : ChatActivity.SHOP_USERNAME;
        boolean ownProduct = username != null && username.equals(seller);
        view.showProduct(product, username, seller, ownProduct, repository.isProductFavorited(username, product.id));
        refreshComments();
    }

    @Override
    public void refreshComments() {
        int count = repository.getProductCommentCount(productId);
        List<ProductComment> comments = repository.getProductComments(productId);
        view.showCommentPreview(count, comments.isEmpty() ? null : comments.get(0));
    }

    @Override
    public void addToCart() {
        if (username == null || username.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        repository.addToCart(username, product);
        view.showToast("已加入购物车");
    }

    @Override
    public void buyNow() {
        if (repository.getDefaultAddress(username) == null) {
            view.showToast("请先添加收货地址");
            view.openAddressManager();
            return;
        }
        repository.addOrder(username, product, 1);
        view.showToast("下单成功");
        view.openMyOrders();
    }

    @Override
    public void toggleFavorite() {
        if (username == null || username.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        boolean favorited = repository.toggleProductFavorite(username, product.id);
        view.setFavoriteState(favorited);
        view.showToast(favorited ? "已收藏商品" : "已取消收藏");
    }

    @Override
    public void deleteProduct() {
        repository.deleteProduct(product.id);
        view.showToast("已下架");
        view.closePage();
    }

    @Override
    public String getStorePhone() {
        return repository.getStorePhone(seller);
    }
}
