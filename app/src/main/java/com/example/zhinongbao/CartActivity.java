package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.CartAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.CartItem;
import com.example.zhinongbao.model.Product;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private CartAdapter adapter;
    private List<CartItem> items;
    private CheckBox cbSelectAll;
    private TextView tvTotal, tvCount;
    private DataManager dm;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        dm = DataManager.getInstance(this);
        username = dm.getLoggedUser();
        items = dm.getCart(username);

        RecyclerView rv = findViewById(R.id.rvCart);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CartAdapter(items);
        adapter.setOnChangeListener(this::refreshBottomBar);
        rv.setAdapter(adapter);

        cbSelectAll = findViewById(R.id.cbSelectAll);
        tvTotal = findViewById(R.id.tvCartTotal);
        tvCount = findViewById(R.id.tvCartCount);
        Button btnCheckout = findViewById(R.id.btnCheckout);
        TextView tvClear = findViewById(R.id.tvClearCart);

        cbSelectAll.setOnCheckedChangeListener((btn, checked) -> adapter.setAllChecked(checked));

        btnCheckout.setOnClickListener(v -> checkout());

        tvClear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("清空购物车")
                .setMessage("确定要清空所有商品吗？")
                .setPositiveButton("清空", (d, w) -> {
                    adapter.clearAll();
                    dm.saveCartPublic(username, items);
                    refreshBottomBar();
                })
                .setNegativeButton("取消", null)
                .show());

        refreshBottomBar();
    }

    private void refreshBottomBar() {
        double total = adapter.getSelectedTotal();
        tvTotal.setText(String.format("¥%.2f", total));
        tvCount.setText("共 " + items.size() + " 件");
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(adapter.areAllChecked());
        cbSelectAll.setOnCheckedChangeListener((btn, checked) -> adapter.setAllChecked(checked));
    }

    private void checkout() {
        List<com.example.zhinongbao.model.CartItem> checkedItems = adapter.getCheckedItems();
        if (checkedItems.isEmpty()) {
            Toast.makeText(this, "请先选择商品", Toast.LENGTH_SHORT).show();
            return;
        }
        for (com.example.zhinongbao.model.CartItem item : checkedItems) {
            Product product = dm.getProductById(item.productId);
            if (product != null && product.seller != null && product.seller.equals(username)) {
                Toast.makeText(this, "不能结算自己发布的商品", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (dm.getDefaultAddress(username) == null) {
            Toast.makeText(this, "请先添加收货地址", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AddressManagerActivity.class));
            return;
        }
        for (com.example.zhinongbao.model.CartItem item : checkedItems) {
            dm.addOrder(username, item.productId, item.name, item.price, item.quantity);
        }
        adapter.removeChecked();
        dm.saveCartPublic(username, items);
        startActivity(new Intent(this, MyOrdersActivity.class));
        finish();
    }
}
