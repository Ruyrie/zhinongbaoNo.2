package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.SellerOrderAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.mvp.sellerorders.SellerOrdersContract;
import com.example.zhinongbao.mvp.sellerorders.SellerOrdersPresenter;
import java.util.ArrayList;
import java.util.List;

public class SellerOrdersActivity extends BaseMvpActivity<SellerOrdersContract.Presenter> implements SellerOrdersContract.View {

    private String currentFilter = "all"; // all, pending, paid, shipped, refund
    private RecyclerView rvOrders;
    private SellerOrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private EditText etOrderSearch;

    private TextView tabAll, tabPending, tabPaid, tabShipped, tabRefund;
    private String salesScope;
    private String orderSearchKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

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
        etOrderSearch = findViewById(R.id.etOrderSearch);
        findViewById(R.id.btnOrderSearch).setOnClickListener(v -> {
            orderSearchKeyword = etOrderSearch.getText().toString().trim();
            presenter.setSearchKeyword(orderSearchKeyword);
            presenter.refresh();
        });

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
            presenter.setFilter(currentFilter);
        };

        tabAll.setOnClickListener(tabListener);
        tabPending.setOnClickListener(tabListener);
        tabPaid.setOnClickListener(tabListener);
        tabShipped.setOnClickListener(tabListener);
        tabRefund.setOnClickListener(tabListener);

        etOrderSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                orderSearchKeyword = s == null ? "" : s.toString().trim();
                presenter.setSearchKeyword(orderSearchKeyword);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        rvOrders = findViewById(R.id.rvSellerOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        new SellerOrdersPresenter(this, this, currentFilter, salesScope);

        adapter = new SellerOrderAdapter(orderList, new SellerOrderAdapter.OnOrderActionListener() {
            @Override
            public void onShip(Order o) {
                presenter.onShipClicked(o);
            }

            @Override
            public void onModifyPrice(Order o) {
                presenter.onModifyPriceClicked(o);
            }

            @Override
            public void onRefund(Order o) {
                presenter.onRefundClicked(o);
            }

            @Override
            public void onContactBuyer(Order o) {
                presenter.onContactBuyer(o);
            }
        }, productId -> presenter.getProductById(productId));
        rvOrders.setAdapter(adapter);

        presenter.start();
        updateTabStyles();
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

    @Override
    public void showOrders(List<Order> orders) {
        orderList.clear();
        orderList.addAll(orders);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openChat(String buyerUser, String productName) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("other_user", buyerUser);
        intent.putExtra("product_name", productName);
        startActivity(intent);
    }

    @Override
    public void showModifyPriceDialog(Order o) {
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
                        presenter.updateOrderPrice(o, u, dist);
                    } catch (Exception e) {
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void showShipDialog(Order o) {
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .create();
        v.findViewById(R.id.btnShipCancel).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btnShipConfirm).setOnClickListener(view -> {
            if (rg.getCheckedRadioButtonId() == R.id.rbExpress) {
                String company = etCompany.getText().toString().trim();
                String no = etNo.getText().toString().trim();
                if (company.isEmpty()) {
                    etCompany.setError("请填写快递公司");
                    return;
                }
                if (no.isEmpty()) {
                    etNo.setError("请填写快递单号");
                    return;
                }
                presenter.shipOrder(o, "express", company, no, "");
            } else {
                String driver = etDriver.getText().toString().trim();
                String car = etCar.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                if (driver.isEmpty()) {
                    etDriver.setError("请填写司机姓名");
                    return;
                }
                if (car.isEmpty()) {
                    etCar.setError("请填写车牌号");
                    return;
                }
                if (phone.isEmpty()) {
                    etPhone.setError("请填写司机联系电话");
                    return;
                }
                presenter.shipOrder(o, "custom", driver, car, phone);
            }
            dialog.dismiss();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Override
    public void showRefundDialog(Order o) {
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
                        presenter.processRefund(o, amt, etReason.getText().toString(), true);
                    } catch (Exception e) {
                    }
                })
                .setNeutralButton("拒绝退款", (d, w) -> {
                    presenter.processRefund(o, 0, etReason.getText().toString(), false);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
