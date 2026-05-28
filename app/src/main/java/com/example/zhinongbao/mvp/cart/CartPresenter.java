package com.example.zhinongbao.mvp.cart;

import android.content.Context;

import com.example.zhinongbao.model.CartItem;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.repository.CartRepository;

import java.util.List;

public class CartPresenter implements CartContract.Presenter {
    private final CartContract.View view;
    private final CartRepository repository;
    private final String username;

    public CartPresenter(Context context, CartContract.View view) {
        this.view = view;
        this.repository = new CartRepository(context.getApplicationContext());
        this.username = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.showCart(repository.getCart(username));
    }

    @Override
    public void onCartSelectionChanged(double total, int itemCount, boolean allChecked) {
        view.updateSummary(total, itemCount, allChecked);
    }

    @Override
    public void onCartItemsChanged(List<CartItem> currentItems) {
        repository.saveCart(username, currentItems);
    }

    @Override
    public void onClearCart(List<CartItem> currentItems) {
        currentItems.clear();
        repository.saveCart(username, currentItems);
        view.updateSummary(0, 0, false);
    }

    @Override
    public void checkout(List<CartItem> currentItems, List<CartItem> checkedItems) {
        if (checkedItems.isEmpty()) {
            view.showToast("请先选择商品");
            return;
        }
        for (CartItem item : checkedItems) {
            Product product = repository.getProductById(item.productId);
            if (product != null && product.seller != null && product.seller.equals(username)) {
                view.showToast("不能结算自己发布的商品");
                return;
            }
        }
        if (repository.getDefaultAddress(username) == null) {
            view.showToast("请先添加收货地址");
            view.openAddressManager();
            return;
        }
        for (CartItem item : checkedItems) {
            repository.addOrder(username, item);
        }
        currentItems.removeAll(checkedItems);
        repository.saveCart(username, currentItems);
        view.openMyOrders();
        view.closePage();
    }
}
