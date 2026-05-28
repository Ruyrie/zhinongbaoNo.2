package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.CartAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.CartItem;
import com.example.zhinongbao.mvp.cart.CartContract;
import com.example.zhinongbao.mvp.cart.CartPresenter;
import com.example.zhinongbao.utils.DialogUtils;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends BaseMvpActivity<CartContract.Presenter> implements CartContract.View {

    private CartAdapter adapter;
    private final List<CartItem> items = new ArrayList<>();
    private CheckBox cbSelectAll;
    private TextView tvTotal, tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvCart);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CartAdapter(items);
        adapter.setOnChangeListener(this::onCartItemsChanged);
        rv.setAdapter(adapter);

        cbSelectAll = findViewById(R.id.cbSelectAll);
        tvTotal = findViewById(R.id.tvCartTotal);
        tvCount = findViewById(R.id.tvCartCount);
        Button btnCheckout = findViewById(R.id.btnCheckout);
        TextView tvClear = findViewById(R.id.tvClearCart);

        cbSelectAll.setOnCheckedChangeListener((btn, checked) -> adapter.setAllChecked(checked));

        btnCheckout.setOnClickListener(v -> checkout());

        tvClear.setOnClickListener(v -> {
            if (items.isEmpty()) {
                showToast("购物车已经是空的");
                return;
            }
            DialogUtils.showConfirm(this,
                    "清空购物车",
                    "将移除购物车中的全部商品，清空后需要重新添加。",
                    "再想想",
                    "确认清空",
                    true,
                    () -> {
                        adapter.clearAll();
                        presenter.onClearCart(items);
                        return true;
                    });
        });

        new CartPresenter(this, this).start();
        refreshBottomBar();
    }

    private void refreshBottomBar() {
        double total = adapter.getSelectedTotal();
        presenter.onCartSelectionChanged(total, items.size(), adapter.areAllChecked());
    }

    private void onCartItemsChanged() {
        refreshBottomBar();
        presenter.onCartItemsChanged(items);
    }

    private void checkout() {
        presenter.checkout(items, adapter.getCheckedItems());
    }

    @Override
    public void showCart(List<CartItem> newItems) {
        items.clear();
        items.addAll(newItems);
        adapter.notifyDataSetChanged();
        refreshBottomBar();
    }

    @Override
    public void updateSummary(double total, int itemCount, boolean allChecked) {
        tvTotal.setText(String.format("¥%.2f", total));
        tvCount.setText("共 " + itemCount + " 件");
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(allChecked);
        cbSelectAll.setOnCheckedChangeListener((btn, checked) -> adapter.setAllChecked(checked));
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openAddressManager() {
        startActivity(new Intent(this, AddressManagerActivity.class));
    }

    @Override
    public void openMyOrders() {
        startActivity(new Intent(this, MyOrdersActivity.class));
    }

    @Override
    public void closePage() {
        finish();
    }
}
