package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.SellerOrderAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Order;
import java.util.ArrayList;
import java.util.List;

public class SellerOrdersActivity extends AppCompatActivity {

    private String currentFilter = "all"; // all, pending, paid, shipped, refund
    private RecyclerView rvOrders;
    private SellerOrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private DataManager dm;

    private TextView tabAll, tabPending, tabPaid, tabShipped, tabRefund;
    private String salesScope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        dm = DataManager.getInstance(this);

        String intentFilter = getIntent().getStringExtra("filter");
        if (intentFilter != null) {
            currentFilter = intentFilter;
        }
        salesScope = getIntent().getStringExtra("sales_scope");

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabPaid = findViewById(R.id.tabPaid);
        tabShipped = findViewById(R.id.tabShipped);
        tabRefund = findViewById(R.id.tabRefund);

        View.OnClickListener tabListener = v -> {
            salesScope = null;
            int id = v.getId();
            if (id == R.id.tabAll)
                currentFilter = "all";
            else if (id == R.id.tabPending)
                currentFilter = "pending";
            else if (id == R.id.tabPaid)
                currentFilter = "paid";
            else if (id == R.id.tabShipped)
                currentFilter = "shipped";
            else if (id == R.id.tabRefund)
                currentFilter = "refund";
            updateTabStyles();
            loadOrders();
        };

        tabAll.setOnClickListener(tabListener);
        tabPending.setOnClickListener(tabListener);
        tabPaid.setOnClickListener(tabListener);
        tabShipped.setOnClickListener(tabListener);
        tabRefund.setOnClickListener(tabListener);

        rvOrders = findViewById(R.id.rvSellerOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SellerOrderAdapter(orderList, new SellerOrderAdapter.OnOrderActionListener() {
            @Override
            public void onShip(Order o) {
                showShipDialog(o);
            }

            @Override
            public void onModifyPrice(Order o) {
                showModifyPriceDialog(o);
            }

            @Override
            public void onRefund(Order o) {
                showRefundDialog(o);
            }

            @Override
            public void onContactBuyer(Order o) {
                Intent intent = new Intent(SellerOrdersActivity.this, ChatActivity.class);
                intent.putExtra("other_user", o.buyerUser);
                intent.putExtra("product_name", o.name);
                startActivity(intent);
            }
        });
        rvOrders.setAdapter(adapter);

        updateTabStyles();
        loadOrders();
    }

    private void updateTabStyles() {
        tabAll.setTextColor("all".equals(currentFilter) ? 0xFF007AFF : 0xFF666666);
        tabPending.setTextColor("pending".equals(currentFilter) ? 0xFF007AFF : 0xFF666666);
        tabPaid.setTextColor("paid".equals(currentFilter) ? 0xFF007AFF : 0xFF666666);
        tabShipped.setTextColor("shipped".equals(currentFilter) ? 0xFF007AFF : 0xFF666666);
        tabRefund.setTextColor("refund".equals(currentFilter) ? 0xFF007AFF : 0xFF666666);

        tabAll.setTypeface(null,
                "all".equals(currentFilter) ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabPending.setTypeface(null,
                "pending".equals(currentFilter) ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabPaid.setTypeface(null,
                "paid".equals(currentFilter) ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabShipped.setTypeface(null,
                "shipped".equals(currentFilter) ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabRefund.setTypeface(null,
                "refund".equals(currentFilter) ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadOrders() {
        String user = dm.getLoggedUser();
        if (user == null)
            return;

        orderList.clear();
        if (salesScope != null) {
            orderList.addAll(dm.getSellerSalesOrders(user, salesScope));
        } else if ("all".equals(currentFilter)) {
            orderList.addAll(dm.getSellerSoldOrders(user));
        } else {
            orderList.addAll(dm.getSellerSoldOrdersByStatus(user, currentFilter));
        }
        adapter.notifyDataSetChanged();
    }

    private void showModifyPriceDialog(Order o) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_seller_price, null);
        TextView tvOrig = v.findViewById(R.id.tvOriginalPrice);
        TextView tvFinal = v.findViewById(R.id.tvFinalPrice);
        EditText etUnit = v.findViewById(R.id.etUnitPrice);
        EditText etDiscount = v.findViewById(R.id.etDiscount);

        tvOrig.setText(String.format("原商品单价: ¥%.2f x %d", o.price, o.quantity));
        double currentUnit = o.unitPrice > 0 ? o.unitPrice : o.price;
        etUnit.setText(String.valueOf(currentUnit));
        etDiscount.setText(String.valueOf(o.discount));

        Runnable calc = () -> {
            try {
                double u = Double.parseDouble(etUnit.getText().toString());
                double d = Double.parseDouble(etDiscount.getText().toString());
                double total = u * o.quantity - d;
                tvFinal.setText(String.format("买家需支付: ¥%.2f", total < 0 ? 0 : total));
            } catch (Exception ignored) {
            }
        };

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calc.run();
            }
        };
        etUnit.addTextChangedListener(tw);
        etDiscount.addTextChangedListener(tw);
        calc.run();

        new AlertDialog.Builder(this)
                .setTitle("修改价格")
                .setView(v)
                .setPositiveButton("确定", (d, w) -> {
                    try {
                        double u = Double.parseDouble(etUnit.getText().toString());
                        double dist = Double.parseDouble(etDiscount.getText().toString());
                        if (dm.updateOrderPrice(o.orderId, u, dist)) {
                            Toast.makeText(this, "改价成功", Toast.LENGTH_SHORT).show();
                            loadOrders();
                        }
                    } catch (Exception e) {
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showShipDialog(Order o) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_seller_ship, null);
        RadioGroup rg = v.findViewById(R.id.rgShipType);
        View llExpress = v.findViewById(R.id.llExpressInputs);
        View llCustom = v.findViewById(R.id.llCustomInputs);

        EditText etCompany = v.findViewById(R.id.etExpressCompany);
        EditText etNo = v.findViewById(R.id.etExpressNo);

        EditText etDriver = v.findViewById(R.id.etDriverName);
        EditText etCar = v.findViewById(R.id.etCarNo);
        EditText etPhone = v.findViewById(R.id.etDriverPhone);

        rg.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.rbExpress) {
                llExpress.setVisibility(View.VISIBLE);
                llCustom.setVisibility(View.GONE);
            } else {
                llExpress.setVisibility(View.GONE);
                llCustom.setVisibility(View.VISIBLE);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("订单发货")
                .setView(v)
                .setPositiveButton("确定发货", (d, w) -> {
                    if (rg.getCheckedRadioButtonId() == R.id.rbExpress) {
                        dm.shipOrder(o.orderId, "express", etCompany.getText().toString(), etNo.getText().toString(),
                                "");
                    } else {
                        dm.shipOrder(o.orderId, "custom", etDriver.getText().toString(), etCar.getText().toString(),
                                etPhone.getText().toString());
                    }
                    Toast.makeText(this, "已发货", Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRefundDialog(Order o) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_partial_refund, null);
        TextView tvInfo = v.findViewById(R.id.tvRefundOrderInfo);
        EditText etAmt = v.findViewById(R.id.etRefundAmount);
        EditText etReason = v.findViewById(R.id.etRefundReason);

        double u = o.unitPrice > 0 ? o.unitPrice : o.price;
        double total = u * o.quantity - o.discount;
        tvInfo.setText(String.format("订单实付: ¥%.2f", total));

        etAmt.setText(String.valueOf(o.refundAmount > 0 ? o.refundAmount : total));
        etReason.setText(o.refundReason);

        new AlertDialog.Builder(this)
                .setTitle("处理售后/退款")
                .setView(v)
                .setPositiveButton("同意退款", (d, w) -> {
                    try {
                        double amt = Double.parseDouble(etAmt.getText().toString());
                        dm.processRefund(o.orderId, amt, etReason.getText().toString(), true);
                        Toast.makeText(this, "已同意退款", Toast.LENGTH_SHORT).show();
                        loadOrders();
                    } catch (Exception e) {
                    }
                })
                .setNeutralButton("拒绝退款", (d, w) -> {
                    dm.processRefund(o.orderId, 0, etReason.getText().toString(), false);
                    Toast.makeText(this, "已拒绝退款，恢复为发货状态", Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
