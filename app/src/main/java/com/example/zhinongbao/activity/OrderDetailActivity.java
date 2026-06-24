package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
    public void showOrder(Order order, double paidAmount, boolean canComment, boolean canRequestRefund,
            String storeName, String storePhone, String storeAvatarUri) {
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
        TextView tvPaidAgain = findViewById(R.id.tvDetailPaidAgain);
        LinearLayout layoutStoreTrace = findViewById(R.id.layoutStoreTrace);
        ImageView ivStoreAvatar = findViewById(R.id.ivDetailStoreAvatar);
        TextView tvStoreAvatar = findViewById(R.id.tvDetailStoreAvatar);
        TextView tvStoreName = findViewById(R.id.tvDetailStoreName);
        TextView tvStorePhone = findViewById(R.id.tvDetailStorePhone);
        TextView btnContactStore = findViewById(R.id.btnDetailContactStore);
        TextView btnOpenStore = findViewById(R.id.btnDetailOpenStore);
        TextView tvOrderType = findViewById(R.id.tvDetailOrderType);
        TextView tvOrderStatus = findViewById(R.id.tvDetailOrderStatus);
        TextView tvOrderQtyInfo = findViewById(R.id.tvDetailOrderQtyInfo);
        LinearLayout layoutShipment = findViewById(R.id.layoutShipmentInfo);
        TextView tvShipType = findViewById(R.id.tvDetailShipType);
        TextView tvShipMain = findViewById(R.id.tvDetailShipMain);
        TextView tvShipNo = findViewById(R.id.tvDetailShipNo);
        TextView tvShipPhone = findViewById(R.id.tvDetailShipPhone);
        LinearLayout layoutReceiver = findViewById(R.id.layoutReceiverInfo);
        TextView tvReceiver = findViewById(R.id.tvDetailReceiver);
        TextView tvAddress = findViewById(R.id.tvDetailAddress);
        LinearLayout layoutActions = findViewById(R.id.layoutDetailActions);
        LinearLayout layoutContactStore = findViewById(R.id.layoutDetailContactStore);
        LinearLayout layoutMore = findViewById(R.id.layoutDetailMore);
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
        tvPaidAgain.setText(String.format("¥%.2f", paidAmount));
        layoutStoreTrace.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(order.seller)) {
            tvStoreName.setText(TextUtils.isEmpty(storeName) ? order.seller + "的店铺" : storeName);
            tvStorePhone.setText(TextUtils.isEmpty(storePhone) ? "联系电话：未填写" : "联系电话：" + storePhone);
            bindStoreAvatar(ivStoreAvatar, tvStoreAvatar, storeAvatarUri, tvStoreName.getText().toString());
            btnContactStore.setEnabled(true);
            btnContactStore.setAlpha(1f);
            btnContactStore.setOnClickListener(v -> openStoreChat());
            layoutContactStore.setOnClickListener(v -> btnContactStore.performClick());
            btnOpenStore.setOnClickListener(v -> {
                Intent intent = new Intent(this, SellerStoreActivity.class);
                intent.putExtra("seller", order.seller);
                intent.putExtra("public_store", true);
                startActivity(intent);
            });
        } else {
            tvStoreName.setText("店铺信息");
            tvStorePhone.setText("联系电话：未填写");
            bindStoreAvatar(ivStoreAvatar, tvStoreAvatar, null, "店铺");
            btnContactStore.setEnabled(false);
            btnContactStore.setAlpha(0.45f);
            layoutContactStore.setOnClickListener(null);
            btnOpenStore.setOnClickListener(null);
        }
        if (order.receiverName != null && !order.receiverName.isEmpty()) {
            layoutReceiver.setVisibility(View.VISIBLE);
            tvReceiver.setText(order.receiverName + "  " + (order.receiverPhone == null ? "" : order.receiverPhone));
            tvAddress.setText(order.receiverAddress == null ? "" : order.receiverAddress);
        } else {
            layoutReceiver.setVisibility(View.GONE);
        }
        tvOrderType.setText(Order.ORDER_TYPE_PROCUREMENT.equals(order.orderType) ? "采购订单" : "零售订单");
        tvOrderStatus.setText(formatStatus(order));
        tvOrderQtyInfo.setText(order.quantity + " 件");
        bindShipmentInfo(layoutShipment, tvShipType, tvShipMain, tvShipNo, tvShipPhone, order);

        switch (order.status) {
            case Order.STATUS_PENDING:
                tvStatus.setText("待支付");
                long rem = order.getRemainingMs();
                long h = rem / 3600000, m = (rem % 3600000) / 60000;
                tvCountdown.setText(String.format("请在 %02d:%02d 内完成支付，逾期将自动取消", h, m));
                layoutActions.setVisibility(View.VISIBLE);
                btnPay.setVisibility(View.VISIBLE);
                btnPay.setText("立即支付");
                setPrimaryButton(btnPay);
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setText("取消订单");
                setSecondaryButton(btnCancel);
                btnComment.setVisibility(View.GONE);
                break;
            case Order.STATUS_PAID:
                tvStatus.setText("待发货");
                tvCountdown.setText("卖家正在准备发货");
                layoutActions.setVisibility(View.VISIBLE);
                btnPay.setVisibility(View.VISIBLE);
                btnPay.setText("申请退款");
                setSecondaryButton(btnPay);
                btnCancel.setVisibility(View.GONE);
                btnComment.setVisibility(View.GONE);
                break;
            case Order.STATUS_SHIPPED:
                tvStatus.setText("待收货");
                tvCountdown.setText("卖家已发货，请确认收货后评价");
                layoutActions.setVisibility(View.VISIBLE);
                btnPay.setVisibility(View.VISIBLE);
                btnPay.setText("确认收货");
                setPrimaryButton(btnPay);
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setText("申请退款");
                setSecondaryButton(btnCancel);
                btnComment.setVisibility(View.GONE);
                break;
            case Order.STATUS_COMPLETED:
                tvStatus.setText(order.refundAmount > 0 ? "已退款" : "已完成");
                tvCountdown.setText(order.refundAmount > 0 ? "退款已完成" : (canComment ? "交易成功，可以评价商品" : "交易成功"));
                layoutActions.setVisibility(order.refundAmount > 0 ? View.GONE : View.VISIBLE);
                btnPay.setVisibility(View.GONE);
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setText(canRequestRefund ? "申请退款" : "联系客服");
                setSecondaryButton(btnCancel);
                btnComment.setVisibility(order.refundAmount > 0 || !canComment ? View.GONE : View.VISIBLE);
                setPrimaryButton(btnComment);
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
        layoutMore.setOnClickListener(v -> showMoreMenu());
    }

    private void bindShipmentInfo(LinearLayout layout, TextView type, TextView main, TextView no, TextView phone,
            Order order) {
        boolean hasShipment = !TextUtils.isEmpty(order.shipName) || !TextUtils.isEmpty(order.shipNo)
                || !TextUtils.isEmpty(order.shipPhone);
        layout.setVisibility(hasShipment ? View.VISIBLE : View.GONE);
        if (!hasShipment) {
            return;
        }
        boolean custom = "custom".equals(order.shipType);
        type.setText(custom ? "自定义货运" : "标准快递");
        main.setText((custom ? "司机姓名：" : "快递公司：") + safeText(order.shipName));
        no.setText((custom ? "车牌号：" : "快递单号：") + safeText(order.shipNo));
        phone.setVisibility(custom ? View.VISIBLE : View.GONE);
        phone.setText("联系电话：" + safeText(order.shipPhone));
    }

    private String safeText(String value) {
        return TextUtils.isEmpty(value) ? "未填写" : value;
    }

    private void setPrimaryButton(Button button) {
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_auth_green_button);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void setSecondaryButton(Button button) {
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_auth_input);
        button.setTextColor(0xFF43A047);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void showMoreMenu() {
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackgroundResource(R.drawable.bg_more_menu_round);
        int horizontal = dp(18);
        menu.setPadding(horizontal, dp(10), horizontal, dp(10));

        PopupWindow popup = new PopupWindow(menu, dp(150), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(8));

        addMenuItem(menu, "加入购物车", () -> {
            popup.dismiss();
            presenter.addToCart();
        });

        View anchor = findViewById(R.id.layoutDetailMore);
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int x = Math.max(dp(12), location[0] - dp(36));
        int y = location[1] - dp(74);
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, Math.max(dp(24), y));
    }

    private void addMenuItem(LinearLayout menu, String text, Runnable action) {
        TextView item = new TextView(this);
        item.setText(text);
        item.setTextColor(0xFF111827);
        item.setTextSize(17);
        item.setGravity(Gravity.CENTER);
        item.setMinHeight(dp(52));
        item.setOnClickListener(v -> action.run());
        menu.addView(item, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void openStoreChat() {
        if (order == null || TextUtils.isEmpty(order.seller)) {
            Toast.makeText(this, "暂无商家联系方式", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("other_user", order.seller);
        intent.putExtra("product_name", order.name);
        startActivity(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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
        DialogUtils.showRefundReason(this, reason -> {
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

    private void bindStoreAvatar(ImageView image, TextView placeholder, String avatarUri, String storeName) {
        if (!TextUtils.isEmpty(avatarUri)) {
            try {
                if (avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(image, avatarUri);
                } else {
                    image.setImageURI(Uri.parse(avatarUri));
                }
                image.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
                return;
            } catch (Exception ignored) {
            }
        }
        image.setVisibility(View.GONE);
        placeholder.setVisibility(View.VISIBLE);
        placeholder.setText(TextUtils.isEmpty(storeName) ? "店" : storeName.substring(0, 1));
    }

    private String formatStatus(Order order) {
        switch (order.status) {
            case Order.STATUS_PENDING:
                return "待支付";
            case Order.STATUS_PAID:
                return "待发货";
            case Order.STATUS_SHIPPED:
                return "待收货";
            case Order.STATUS_COMPLETED:
                return order.refundAmount > 0 ? "已退款" : "已完成";
            case Order.STATUS_REFUND:
                return "售后中";
            case Order.STATUS_CANCELLED:
                return "已取消";
            default:
                return "未知状态";
        }
    }

    @Override
    public void closePage() {
        finish();
    }
}
