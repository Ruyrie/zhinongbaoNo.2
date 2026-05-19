package com.example.zhinongbao;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Order;

public class OrderDetailActivity extends AppCompatActivity {

    private Order order;
    private DataManager dm;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        dm = DataManager.getInstance(this);
        username = dm.getLoggedUser();
        String orderId = getIntent().getStringExtra("order_id");

        for (Order o : dm.getOrders(username)) {
            if (o.orderId.equals(orderId)) {
                order = o;
                break;
            }
        }
        if (order == null) {
            finish();
            return;
        }

        bind();
    }

    private void bind() {
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        TextView tvCountdown = findViewById(R.id.tvDetailCountdown);
        ImageView ivProduct = findViewById(R.id.ivDetailProduct);
        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvPrice = findViewById(R.id.tvDetailPrice);
        TextView tvQty = findViewById(R.id.tvDetailQty);
        TextView tvOrderId = findViewById(R.id.tvDetailOrderId);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        TextView tvTotal = findViewById(R.id.tvDetailTotal);
        LinearLayout layoutActions = findViewById(R.id.layoutDetailActions);
        Button btnPay = findViewById(R.id.btnDetailPay);
        Button btnCancel = findViewById(R.id.btnDetailCancel);
        Button btnComment = findViewById(R.id.btnDetailComment);

        tvName.setText(order.name);
        switch (order.productId) {
            case 1:
                ivProduct.setImageResource(R.mipmap.dami1);
                break;
            case 2:
                ivProduct.setImageResource(R.mipmap.muer);
                break;
            case 3:
                ivProduct.setImageResource(R.mipmap.fengmi1);
                break;
            case 4:
                ivProduct.setImageResource(R.mipmap.shucai1);
                break;
            case 5:
                ivProduct.setImageResource(R.mipmap.dongchongxiacao1);
                break;
            case 6:
                ivProduct.setImageResource(R.mipmap.hongshu1);
                break;
            case 7:
                ivProduct.setImageResource(R.mipmap.shanyao1);
                break;
            case 8:
                ivProduct.setImageResource(R.mipmap.yangdujun1);
                break;
            case 9:
                ivProduct.setImageResource(R.mipmap.luronggu1);
                break;
            case 10:
                ivProduct.setImageResource(R.mipmap.tuedan1);
                break;
            default:
                ivProduct.setImageResource(R.drawable.ic_product_placeholder);
        }
        tvPrice.setText(String.format("¥%.2f", order.price));
        tvQty.setText("x" + order.quantity);
        tvOrderId.setText(order.orderId);
        tvTime.setText(order.time);
        tvTotal.setText(String.format("¥%.2f", order.price * order.quantity));

        // 若 pending 且已超时，自动取消
        if (Order.STATUS_PENDING.equals(order.status) && order.getRemainingMs() <= 0) {
            dm.updateOrderStatus(username, order.orderId, Order.STATUS_CANCELLED);
            order.status = Order.STATUS_CANCELLED;
        }

        switch (order.status) {
            case Order.STATUS_PENDING:
                tvStatus.setText("待支付");
                long rem = order.getRemainingMs();
                long h = rem / 3600000, m = (rem % 3600000) / 60000;
                tvCountdown.setText(String.format("请在 %02d:%02d 内完成支付，逾期将自动取消", h, m));
                layoutActions.setVisibility(View.VISIBLE);
                btnPay.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                btnComment.setVisibility(View.GONE);
                break;
            case Order.STATUS_PAID:
                tvStatus.setText("已完成");
                tvCountdown.setText("感谢您的购买");
                layoutActions.setVisibility(View.VISIBLE);
                btnPay.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
                btnComment.setVisibility(View.VISIBLE);
                break;
            case Order.STATUS_CANCELLED:
                tvStatus.setText("已取消");
                tvCountdown.setText("订单已取消");
                tvStatus.getParent();
                ((View) tvStatus.getParent()).setBackgroundColor(0xFF8A8A8A);
                layoutActions.setVisibility(View.GONE);
                break;
        }

        btnComment.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, ProductCommentsActivity.class);
            intent.putExtra("product_id", order.productId);
            startActivity(intent);
        });

        btnPay.setOnClickListener(v -> {
            dm.updateOrderStatus(username, order.orderId, Order.STATUS_PAID);
            Toast.makeText(this, "支付成功！", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnCancel.setOnClickListener(v -> {
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

            view.findViewById(R.id.btnDialogCancel).setOnClickListener(v1 -> dialog.dismiss());
            view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v1 -> {
                dialog.dismiss();
                dm.updateOrderStatus(username, order.orderId, Order.STATUS_CANCELLED);
                Toast.makeText(this, "订单已取消", Toast.LENGTH_SHORT).show();
                finish();
            });
            dialog.show();
        });
    }
}
