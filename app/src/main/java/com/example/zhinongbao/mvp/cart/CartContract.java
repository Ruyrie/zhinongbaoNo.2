package com.example.zhinongbao.mvp.cart;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.CartItem;

import java.util.List;

public interface CartContract {
    interface View extends BaseView<Presenter> {
        void showCart(List<CartItem> items);

        void updateSummary(double total, int itemCount, boolean allChecked);

        void showToast(String message);

        void openAddressManager();

        void openMyOrders();

        void closePage();
    }

    interface Presenter extends BasePresenter {
        void onCartSelectionChanged(double total, int itemCount, boolean allChecked);

        void onClearCart(List<CartItem> currentItems);

        void checkout(List<CartItem> currentItems, List<CartItem> checkedItems);
    }
}
