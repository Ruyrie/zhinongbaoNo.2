package com.example.zhinongbao.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zhinongbao.R;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.repository.OrderRepository;

import java.util.Locale;

/**
 * 卖家侧「订单详情」页面。
 * 与买家版 {@link OrderDetailActivity} 区分：这里展示买家信息、收货地址等卖家需要的内容，
 * 底部提供卖家操作（去发货 / 处理售后 / 联系买家），而不是买家的「申请退款 / 进店逛逛 / 客服」。
 */
public class SellerOrderDetailActivity extends AppCompatActivity {

    private OrderRepository orderRepository;
    private String orderId;
    private Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_order_detail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        orderRepository = new OrderRepository(getApplicationContext());
        orderId = getIntent().getStringExtra("order_id");

        findViewById(R.id.ivSellerOdBack).setOnClickListener(v -> finish());
        bind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 发货/处理售后后回到本页时刷新最新状态
        if (orderId != null) {
            bind();
        }
    }

    private void bind() {
        order = orderRepository.getOrderById(orderId);
        if (order == null) {
            Toast.makeText(this, "订单不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 状态
        ((TextView) findViewById(R.id.tvSellerOdStatus)).setText(statusLabel(order));
        ((TextView) findViewById(R.id.tvSellerOdStatusHint)).setText(statusHint(order));

        // 买家信息
        String buyer = TextUtils.isEmpty(order.buyerNickname) ? order.buyerUser : order.buyerNickname;
        ((TextView) findViewById(R.id.tvSellerOdBuyer)).setText("买家：" + (buyer == null ? "—" : buyer));
        findViewById(R.id.btnSellerOdContactBuyer).setOnClickListener(v -> contactBuyer());

        // 商品
        ((TextView) findViewById(R.id.tvSellerOdName)).setText(order.name);
        ((TextView) findViewById(R.id.tvSellerOdPrice))
                .setText(String.format(Locale.getDefault(), "¥%.2f", order.getEffectiveUnitPrice()));
        ((TextView) findViewById(R.id.tvSellerOdQty)).setText("x" + order.quantity);
        bindProductImage((ImageView) findViewById(R.id.ivSellerOdProduct));

        // 金额：商品总价（折扣前）/ 实付款（折扣后）
        ((TextView) findViewById(R.id.tvSellerOdTotal))
                .setText(String.format(Locale.getDefault(), "¥%.2f", order.getSubtotal()));
        ((TextView) findViewById(R.id.tvSellerOdPaid))
                .setText(String.format(Locale.getDefault(), "¥%.2f", order.getPayableAmount()));

        // 订单信息
        ((TextView) findViewById(R.id.tvSellerOdOrderId)).setText(order.orderId);
        ((TextView) findViewById(R.id.tvSellerOdTime)).setText(order.time);
        ((TextView) findViewById(R.id.tvSellerOdType))
                .setText(Order.ORDER_TYPE_PROCUREMENT.equals(order.orderType) ? "采购订单" : "零售订单");
        ((TextView) findViewById(R.id.tvSellerOdStatusInfo)).setText(statusLabel(order));
        ((TextView) findViewById(R.id.tvSellerOdQtyInfo)).setText(order.quantity + " 件");

        // 收货信息
        LinearLayout layoutReceiver = findViewById(R.id.layoutSellerOdReceiver);
        if (!TextUtils.isEmpty(order.receiverName)) {
            layoutReceiver.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvSellerOdReceiver))
                    .setText(order.receiverName + "  " + safeText(order.receiverPhone));
            ((TextView) findViewById(R.id.tvSellerOdAddress)).setText(safeText(order.receiverAddress));
        } else {
            layoutReceiver.setVisibility(View.GONE);
        }

        bindShipmentInfo();
        bindActions();
    }

    private void bindProductImage(ImageView iv) {
        // 采购订单优先展示买家上传的需求图片
        String image = firstImage(order.proofImages);
        int defaultRes = defaultProductImageRes(order.productId);
        if (image != null) {
            try {
                iv.setImageURI(android.net.Uri.parse(image));
                return;
            } catch (Exception ignored) {
            }
        }
        if (defaultRes != 0) {
            iv.setImageResource(defaultRes);
        } else {
            iv.setImageResource(R.drawable.ic_product_placeholder);
        }
    }

    private void bindShipmentInfo() {
        LinearLayout layout = findViewById(R.id.layoutSellerOdShip);
        boolean hasShipment = !TextUtils.isEmpty(order.shipName) || !TextUtils.isEmpty(order.shipNo)
                || !TextUtils.isEmpty(order.shipPhone);
        layout.setVisibility(hasShipment ? View.VISIBLE : View.GONE);
        if (!hasShipment) {
            return;
        }
        boolean custom = "custom".equals(order.shipType);
        ((TextView) findViewById(R.id.tvSellerOdShipType)).setText(custom ? "自定义货运" : "标准快递");
        ((TextView) findViewById(R.id.tvSellerOdShipMain))
                .setText((custom ? "司机姓名：" : "快递公司：") + safeText(order.shipName));
        ((TextView) findViewById(R.id.tvSellerOdShipNo))
                .setText((custom ? "车牌号：" : "快递单号：") + safeText(order.shipNo));
        TextView phone = findViewById(R.id.tvSellerOdShipPhone);
        phone.setVisibility(custom ? View.VISIBLE : View.GONE);
        phone.setText("联系电话：" + safeText(order.shipPhone));
    }

    // 底部操作：按订单状态给卖家相应动作
    private void bindActions() {
        LinearLayout actions = findViewById(R.id.layoutSellerOdActions);
        Button primary = findViewById(R.id.btnSellerOdPrimary);
        Button secondary = findViewById(R.id.btnSellerOdSecondary);
        // 统一按钮样式：主操作=绿底白字，次操作=浅底绿字（清除 Material 默认着色，避免两个按钮都被染成同色）
        stylePrimaryButton(primary);
        styleSecondaryButton(secondary);
        primary.setVisibility(View.GONE);
        secondary.setVisibility(View.GONE);

        switch (order.status) {
            case Order.STATUS_PAID: // 待发货
                primary.setVisibility(View.VISIBLE);
                primary.setText("去发货");
                primary.setOnClickListener(v -> showShipDialog());
                secondary.setVisibility(View.VISIBLE);
                secondary.setText("联系买家");
                secondary.setOnClickListener(v -> contactBuyer());
                break;
            case Order.STATUS_REFUND: // 待处理售后
                primary.setVisibility(View.VISIBLE);
                primary.setText("处理售后");
                primary.setOnClickListener(v -> showRefundDialog());
                secondary.setVisibility(View.VISIBLE);
                secondary.setText("联系买家");
                secondary.setOnClickListener(v -> contactBuyer());
                break;
            case Order.STATUS_SHIPPED: // 已发货
            case Order.STATUS_PENDING: // 待付款
                secondary.setVisibility(View.VISIBLE);
                secondary.setText("联系买家");
                secondary.setOnClickListener(v -> contactBuyer());
                break;
            default: // 已完成 / 已取消：无操作
                break;
        }
        boolean anyVisible = primary.getVisibility() == View.VISIBLE
                || secondary.getVisibility() == View.VISIBLE;
        actions.setVisibility(anyVisible ? View.VISIBLE : View.GONE);
    }

    // 主操作按钮：绿底白字（去发货 / 处理售后）
    private void stylePrimaryButton(Button button) {
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_auth_green_button);
        button.setTextColor(0xFFFFFFFF);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    // 次操作按钮：浅底绿字描边（联系买家）
    private void styleSecondaryButton(Button button) {
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_auth_input);
        button.setTextColor(0xFF43A047);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void contactBuyer() {
        if (order == null || TextUtils.isEmpty(order.buyerUser)) {
            Toast.makeText(this, "暂无买家联系方式", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("other_user", order.buyerUser);
        intent.putExtra("product_name", order.name);
        startActivity(intent);
    }

    // 发货弹窗（与卖家订单列表一致：快递必填公司+单号，自送必填司机/车牌/电话）
    private void showShipDialog() {
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
            boolean express = id == R.id.rbExpress;
            llExpress.setVisibility(express ? View.VISIBLE : View.GONE);
            llCustom.setVisibility(express ? View.GONE : View.VISIBLE);
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        v.findViewById(R.id.btnShipCancel).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btnShipConfirm).setOnClickListener(view -> {
            boolean ok;
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
                ok = orderRepository.shipOrder(order.orderId, "express", company, no, "");
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
                ok = orderRepository.shipOrder(order.orderId, "custom", driver, car, phone);
            }
            dialog.dismiss();
            if (ok) {
                Toast.makeText(this, "已发货", Toast.LENGTH_SHORT).show();
                bind();
            }
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    // 处理售后弹窗（同意按金额退款 / 拒绝）
    private void showRefundDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_partial_refund, null);
        TextView tvProduct = v.findViewById(R.id.tvRefundProduct);
        TextView tvOrderNo = v.findViewById(R.id.tvRefundOrderNo);
        TextView tvInfo = v.findViewById(R.id.tvRefundOrderInfo);
        TextView tvReason = v.findViewById(R.id.tvRefundReasonText);
        TextView tvCountdown = v.findViewById(R.id.tvRefundCountdown);
        TextView tvHint = v.findViewById(R.id.tvRefundAmountHint);
        EditText etAmt = v.findViewById(R.id.etRefundAmount);

        final double paid = order.getPayableAmount();
        tvProduct.setText(order.name + "  x" + order.quantity);
        tvOrderNo.setText("订单号：" + order.orderId);
        tvInfo.setText(String.format(Locale.getDefault(), "¥%.2f", paid));
        tvReason.setText(TextUtils.isEmpty(order.refundReason) ? "买家申请退款" : order.refundReason);
        tvHint.setText(String.format(Locale.getDefault(), "可退金额范围：¥0.00 ~ ¥%.2f", paid));
        etAmt.setText(String.format(Locale.getDefault(), "%.2f", order.refundAmount > 0 ? order.refundAmount : paid));

        if (order.refundRequestedAt > 0) {
            long remaining = order.refundRequestedAt + 24L * 60 * 60 * 1000 - System.currentTimeMillis();
            if (remaining > 0) {
                long h = remaining / 3600000;
                long m = (remaining % 3600000) / 60000;
                tvCountdown.setText(String.format(Locale.getDefault(),
                        "剩余处理时间 %02d:%02d，超时将自动退款", h, m));
                tvCountdown.setVisibility(View.VISIBLE);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        v.findViewById(R.id.btnRefundCancel).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btnRefundReject).setOnClickListener(view -> {
            orderRepository.processRefund(order.orderId, 0, order.refundReason, false);
            dialog.dismiss();
            Toast.makeText(this, "已拒绝退款申请", Toast.LENGTH_SHORT).show();
            bind();
        });
        v.findViewById(R.id.btnRefundApprove).setOnClickListener(view -> {
            double amt;
            try {
                amt = Double.parseDouble(etAmt.getText().toString().trim());
            } catch (Exception e) {
                etAmt.setError("请输入有效的退款金额");
                return;
            }
            if (amt <= 0 || amt > paid) {
                etAmt.setError(String.format(Locale.getDefault(), "退款金额需在 0 ~ %.2f 之间", paid));
                return;
            }
            orderRepository.processRefund(order.orderId, amt, order.refundReason, true);
            dialog.dismiss();
            Toast.makeText(this, "已同意退款", Toast.LENGTH_SHORT).show();
            bind();
        });
        dialog.show();
    }

    private String statusLabel(Order o) {
        switch (o.status) {
            case Order.STATUS_PENDING:
                return "待付款";
            case Order.STATUS_PAID:
                return "待发货";
            case Order.STATUS_SHIPPED:
                return "已发货";
            case Order.STATUS_COMPLETED:
                return o.refundAmount > 0 ? "已退款" : "已完成";
            case Order.STATUS_REFUND:
                return "待处理售后";
            case Order.STATUS_CANCELLED:
                return "已取消";
            default:
                return o.status;
        }
    }

    private String statusHint(Order o) {
        switch (o.status) {
            case Order.STATUS_PENDING:
                return "等待买家付款";
            case Order.STATUS_PAID:
                return "买家已付款，请尽快发货";
            case Order.STATUS_SHIPPED:
                return "已发货，等待买家确认收货";
            case Order.STATUS_COMPLETED:
                return o.refundAmount > 0 ? "退款已完成" : "交易已完成";
            case Order.STATUS_REFUND:
                return "买家申请退款，请及时处理";
            case Order.STATUS_CANCELLED:
                return "订单已取消";
            default:
                return "";
        }
    }

    private String firstImage(String images) {
        if (TextUtils.isEmpty(images)) {
            return null;
        }
        String first = images.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }

    private String safeText(String value) {
        return TextUtils.isEmpty(value) ? "未填写" : value;
    }

    private int defaultProductImageRes(int productId) {
        switch (productId) {
            case 1:
                return R.mipmap.dami1;
            case 2:
                return R.mipmap.muer;
            case 3:
                return R.mipmap.fengmi1;
            case 4:
                return R.mipmap.shucai1;
            case 5:
                return R.mipmap.dongchongxiacao1;
            case 6:
                return R.mipmap.hongshu1;
            case 7:
                return R.mipmap.shanyao1;
            case 8:
                return R.mipmap.yangdujun1;
            case 9:
                return R.mipmap.luronggu1;
            case 10:
                return R.mipmap.tuedan1;
            default:
                return 0;
        }
    }
}
