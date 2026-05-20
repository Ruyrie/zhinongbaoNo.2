package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.OrderAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Order;
import java.util.List;

public class MyOrdersActivity extends AppCompatActivity {

    private List<Order> orders;
    private OrderAdapter adapter;
    private DataManager dm;
    private String username;
    private String filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        dm = DataManager.getInstance(this);
        username = dm.getLoggedUser();
        filter = getIntent().getStringExtra("filter");
        orders = loadOrders();
        updateTitle();

        RecyclerView rv = findViewById(R.id.rvOrders);
        rv.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        adapter = new OrderAdapter(orders);
        adapter.setReviewMode("reviewing".equals(filter));

        adapter.setOnItemClickListener(order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("order_id", order.orderId);
            startActivity(intent);
        });

        adapter.setOnActionListener(new OrderAdapter.OnActionListener() {
            @Override
            public void onPay(Order order) {
                dm.updateOrderStatus(username, order.orderId, Order.STATUS_PAID);
                refresh();
                android.widget.Toast.makeText(MyOrdersActivity.this, "支付成功！", android.widget.Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel(Order order) {
                android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
                android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
                android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
                tvTitle.setText("取消订单");
                tvMessage.setText("确定取消此订单吗？");

                AlertDialog dialog = new AlertDialog.Builder(MyOrdersActivity.this)
                        .setView(view)
                        .create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }

                view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
                view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
                    dialog.dismiss();
                    dm.updateOrderStatus(username, order.orderId, Order.STATUS_CANCELLED);
                    refresh();
                });
                dialog.show();
            }

            @Override
            public void onReview(Order order) {
                Intent intent = new Intent(MyOrdersActivity.this, AddProductCommentActivity.class);
                intent.putExtra("product_id", order.productId);
                startActivity(intent);
            }

            @Override
            public void onConfirmReceipt(Order order) {
                dm.confirmReceipt(username, order.orderId);
                android.widget.Toast.makeText(MyOrdersActivity.this, "已确认收货，现在可以评价商品", android.widget.Toast.LENGTH_SHORT).show();
                refresh();
            }

            @Override
            public void onRequestRefund(Order order) {
                android.widget.EditText etReason = new android.widget.EditText(MyOrdersActivity.this);
                etReason.setHint("请输入退款原因");
                etReason.setMinLines(2);
                etReason.setPadding(32, 12, 32, 12);
                new AlertDialog.Builder(MyOrdersActivity.this)
                        .setTitle("申请退款")
                        .setMessage("退款申请提交后，卖家 24 小时内未处理将自动退款。")
                        .setView(etReason)
                        .setPositiveButton("提交申请", (dialog, which) -> {
                            String reason = etReason.getText().toString().trim();
                            if (reason.isEmpty())
                                reason = "买家申请退款";
                            dm.initiateRefund(order.orderId, reason);
                            android.widget.Toast.makeText(MyOrdersActivity.this, "退款申请已提交", android.widget.Toast.LENGTH_SHORT).show();
                            refresh();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        orders.clear();
        orders.addAll(loadOrders());
        adapter.notifyDataSetChanged();
    }

    private List<Order> loadOrders() {
        if ("reviewing".equals(filter)) {
            return dm.getPendingReviewOrders(username);
        }
        java.util.ArrayList<Order> result = new java.util.ArrayList<>();
        for (Order order : dm.getOrders(username)) {
            if (filter == null || filter.isEmpty()) {
                result.add(order);
            } else if ("shipping".equals(filter) && Order.STATUS_PAID.equals(order.status)) {
                result.add(order);
            } else if ("receiving".equals(filter) && Order.STATUS_SHIPPED.equals(order.status)) {
                result.add(order);
            } else if ("pending".equals(filter) && Order.STATUS_PENDING.equals(order.status)) {
                result.add(order);
            } else if ("refund".equals(filter) && Order.STATUS_REFUND.equals(order.status)) {
                result.add(order);
            }
        }
        return result;
    }

    private void updateTitle() {
        android.widget.TextView title = findViewById(R.id.tvOrderListTitle);
        if (title == null)
            return;
        if ("reviewing".equals(filter)) {
            title.setText("待评价");
        } else {
            title.setText("我的订单");
        }
    }
}
