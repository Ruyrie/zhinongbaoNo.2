package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.OrderAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.mvp.myorders.MyOrdersContract;
import com.example.zhinongbao.mvp.myorders.MyOrdersPresenter;
import com.example.zhinongbao.utils.DialogUtils;
import java.util.ArrayList;
import java.util.List;

public class MyOrdersActivity extends BaseMvpActivity<MyOrdersContract.Presenter> implements MyOrdersContract.View {

    private final List<Order> orders = new ArrayList<>();
    private OrderAdapter adapter;
    private String filter = "all";
    private TextView tabAll, tabPending, tabShipping, tabReceiving, tabReviewing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        String intentFilter = getIntent().getStringExtra("filter");
        if (intentFilter != null && !intentFilter.isEmpty()) {
            filter = intentFilter;
        }
        updateTitle();
        bindTabs();

        RecyclerView rv = findViewById(R.id.rvOrders);
        rv.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        adapter = new OrderAdapter(orders);
        adapter.setReviewMode("reviewing".equals(filter));

        adapter.setOnItemClickListener(order -> presenter.onOrderClicked(order));

        adapter.setOnActionListener(new OrderAdapter.OnActionListener() {
            @Override
            public void onPay(Order order) {
                presenter.onPay(order);
            }

            @Override
            public void onCancel(Order order) {
                presenter.onCancel(order);
            }

            @Override
            public void onReview(Order order) {
                presenter.onReview(order);
            }

            @Override
            public void onConfirmReceipt(Order order) {
                presenter.onConfirmReceipt(order);
            }

            @Override
            public void onRequestRefund(Order order) {
                presenter.onRequestRefund(order);
            }
        });

        rv.setAdapter(adapter);
        new MyOrdersPresenter(this, this, filter).start();
        updateTabStyles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.refresh();
        }
    }

    @Override
    public void showOrders(List<Order> newOrders) {
        orders.clear();
        orders.addAll(newOrders);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showCancelConfirm(Order order) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        tvTitle.setText("取消订单");
        tvMessage.setText("确定取消此订单吗？");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            presenter.onCancelConfirmed(order);
        });
        dialog.show();
    }

    @Override
    public void showRefundDialog(Order order) {
        DialogUtils.showRefundReason(this, reason -> {
            presenter.onRefundConfirmed(order, reason);
            return true;
        });
    }

    @Override
    public void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openOrderDetail(String orderId) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("order_id", orderId);
        startActivity(intent);
    }

    @Override
    public void openReview(int productId) {
        Intent intent = new Intent(this, AddProductCommentActivity.class);
        intent.putExtra("product_id", productId);
        startActivity(intent);
    }

    @Override
    public void openStoreChat(Order order) {
        if (order == null || order.seller == null || order.seller.isEmpty()) {
            showToast("暂无商家联系方式");
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("other_user", order.seller);
        intent.putExtra("product_name", order.name);
        startActivity(intent);
    }

    private void updateTitle() {
        TextView title = findViewById(R.id.tvOrderListTitle);
        if (title == null)
            return;
        title.setText("refundable".equals(filter) ? "可退款/售后订单" : "我的订单");
    }

    private void bindTabs() {
        tabAll = findViewById(R.id.tabOrderAll);
        tabPending = findViewById(R.id.tabOrderPending);
        tabShipping = findViewById(R.id.tabOrderShipping);
        tabReceiving = findViewById(R.id.tabOrderReceiving);
        tabReviewing = findViewById(R.id.tabOrderReviewing);

        tabAll.setOnClickListener(v -> switchFilter("all"));
        tabPending.setOnClickListener(v -> switchFilter("pending"));
        tabShipping.setOnClickListener(v -> switchFilter("shipping"));
        tabReceiving.setOnClickListener(v -> switchFilter("receiving"));
        tabReviewing.setOnClickListener(v -> switchFilter("reviewing"));
    }

    private void switchFilter(String nextFilter) {
        filter = nextFilter;
        adapter.setReviewMode("reviewing".equals(filter));
        updateTitle();
        updateTabStyles();
        presenter.setFilter(filter);
    }

    private void updateTabStyles() {
        styleTab(tabAll, "all".equals(filter));
        styleTab(tabPending, "pending".equals(filter));
        styleTab(tabShipping, "shipping".equals(filter));
        styleTab(tabReceiving, "receiving".equals(filter));
        styleTab(tabReviewing, "reviewing".equals(filter));
    }

    private void styleTab(TextView tab, boolean selected) {
        if (tab == null) {
            return;
        }
        tab.setTextColor(selected ? 0xFF43A047 : 0xFF666666);
        tab.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }
}
