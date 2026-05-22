package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.mvp.orderdetail.OrderDetailContract;
import com.example.zhinongbao.mvp.orderdetail.OrderDetailPresenter;
import com.example.zhinongbao.utils.DialogUtils;

public class OrderDetailActivity extends BaseMvpActivity<OrderDetailContract.Presenter> implements OrderDetailContract.View {

    private Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        String orderId = getIntent().getStringExtra("order_id");
        new OrderDetailPresenter(this, this, orderId).start();
    }

    @Override
    public void showOrder(Order order, double paidAmount, boolean canComment) {
        this.order = order;
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        TextView tvCountdown = findViewById(R.id.tvDetailCountdown);
        ImageView ivProduct = findViewById(R.id.ivDetailProduct);
        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvPrice = findViewById(R.id.tvDetailPrice);
        TextView tvQty = findViewById(R.id.tvDetailQty);
        TextView tvOrderId = findViewById(R.id.tvDetailOrderId);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        TextView tvTotal = findViewById(R.id.tvDetailTotal);
        LinearLayout layoutReceiver = findViewById(R.id.layoutReceiverInfo);
        TextView tvReceiver = findViewById(R.id.tvDetailReceiver);
        TextView tvAddress = findViewById(R.id.tvDetailAddress);
        LinearLayout layoutActions = findViewById(R.id.layoutDetailActions);
        Button btnPay = findViewById(R.id.btnDetailPay);
        Button btnCancel = findViewById(R.id.btnDetailCancel);
        Button btnComment = findViewById(R.id.btnDetailComment);

        tvName.setText(order.name);
        if (!TextUtils.isEmpty(order.proofImages)) {
            ivProduct.setImageURI(Uri.parse(firstImage(order.proofImages)));
        } else switch (order.productId) {
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
        tvTotal.setText(String.format("¥%.2f", paidAmount));
        if (order.receiverName != null && !order.receiverName.isEmpty()) {
            layoutReceiver.setVisibility(View.VISIBLE);
            tvReceiver.setText(order.receiverName + "  " + (order.receiverPhone == null ? "" : order.receiverPhone));
            tvAddress.setText(order.receiverAddress == null ? "" : order.receiverAddress);
        } else {
            layoutReceiver.setVisibility(View.GONE);
        }

        switch (order.status) {
            case Order.STATUS_PENDING:
                tvStatus.setText("待支付");
                long rem = order.getRemainingMs();
                long h = rem / 3600000, m = (rem % 3600000) / 60000;
                tvCountdown.setText(String.format("请在 %02d:%02d 内完成支付，逾期将自动取消", h, m));
                layoutActions.setVisibility(View.VISIBLE);
                btnPay.setVisibility(View.VISIBLE);
                btnPay.setText("立即支付");
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setText("取消订单");
                btnComment.setVisibility(View.GONE);
                break;
            case Order.STATUS_PAID:
                tvStatus.setText("待发货");
                tvCountdown.setText("卖家正在准备发货");
                layoutActions.setVisibility(View.VISIBLE);
                btnPay.setVisibility(View.VISIBLE);
                btnPay.setText("申请退款");
                btnCancel.setVisibility(View.GONE);
                btnComment.setVisibility(View.GONE);
                break;
            case Order.STATUS_SHIPPED:
                tvStatus.setText("待收货");
                tvCountdown.setText("卖家已发货，请确认收货后评价");
                layoutActions.setVisibility(View.VISIBLE);
                btnPay.setVisibility(View.VISIBLE);
                btnPay.setText("确认收货");
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setText("申请退款");
                btnComment.setVisibility(View.GONE);
                break;
            case Order.STATUS_COMPLETED:
                tvStatus.setText(order.refundAmount > 0 ? "已退款" : "已完成");
                tvCountdown.setText(order.refundAmount > 0 ? "退款已完成" : (canComment ? "交易成功，可以评价商品" : "交易成功"));
                layoutActions.setVisibility(order.refundAmount > 0 || !canComment ? View.GONE : View.VISIBLE);
                btnPay.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
                btnComment.setVisibility(order.refundAmount > 0 || !canComment ? View.GONE : View.VISIBLE);
                break;
            case Order.STATUS_REFUND:
                tvStatus.setText("售后中");
                tvCountdown.setText("退款申请处理中，卖家24小时未处理将自动退款");
                layoutActions.setVisibility(View.GONE);
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
            Intent intent = new Intent(this, AddProductCommentActivity.class);
            intent.putExtra("product_id", order.productId);
            startActivity(intent);
        });

        btnPay.setOnClickListener(v -> presenter.onPrimaryAction());
        btnCancel.setOnClickListener(v -> presenter.onSecondaryAction());
    }

    @Override
    public void showCancelConfirm() {
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
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
            presenter.confirmCancel();
        });
        dialog.show();
    }

    @Override
    public void showRefundDialog() {
        DialogUtils.showTextInput(this, "申请退款",
                "退款申请提交后，卖家 24 小时内未处理将自动退款。",
                "请输入退款原因", "取消", "提交申请", false, reason -> {
                    presenter.requestRefund(reason);
                    return true;
                });
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String firstImage(String images) {
        if (TextUtils.isEmpty(images)) {
            return "";
        }
        String[] parts = images.split(",");
        return parts.length == 0 ? "" : parts[0].trim();
    }

    @Override
    public void closePage() {
        finish();
    }
}
