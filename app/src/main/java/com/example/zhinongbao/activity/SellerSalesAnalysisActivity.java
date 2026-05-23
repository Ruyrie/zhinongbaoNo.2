package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.mvp.sellersales.SellerSalesContract;
import com.example.zhinongbao.mvp.sellersales.SellerSalesPresenter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerSalesAnalysisActivity extends BaseMvpActivity<SellerSalesContract.Presenter>
        implements SellerSalesContract.View {

    private static class ProductSummary {
        int quantity;
        double amount;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_sales_analysis);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        new SellerSalesPresenter(this, this, getIntent().getStringExtra("sales_scope")).start();
    }

    @Override
    public void showSales(String scope, List<Order> orders, double total) {
        Map<String, ProductSummary> productMap = new LinkedHashMap<>();
        for (Order order : orders) {
            double amount = presenter.getOrderPaidAmount(order) - order.refundAmount;
            if (amount < 0)
                amount = 0;

            ProductSummary summary = productMap.get(order.name);
            if (summary == null) {
                summary = new ProductSummary();
                productMap.put(order.name, summary);
            }
            summary.quantity += order.quantity;
            summary.amount += amount;
        }

        ((TextView) findViewById(R.id.tvTitle)).setText(scopeName(scope) + "销售流水分析");
        ((TextView) findViewById(R.id.tvScope)).setText(scopeDesc(scope));
        ((TextView) findViewById(R.id.tvRevenue)).setText(String.format(Locale.getDefault(), "¥%.2f", total));
        ((TextView) findViewById(R.id.tvOrderCount)).setText("到账订单 " + orders.size() + " 笔");
        double avg = orders.isEmpty() ? 0 : total / orders.size();
        ((TextView) findViewById(R.id.tvAvgAmount)).setText(String.format(Locale.getDefault(), "客单价 ¥%.2f", avg));

        bindProductSummary(productMap);
        bindOrderFlow(orders);
    }

    private void bindProductSummary(Map<String, ProductSummary> productMap) {
        LinearLayout container = findViewById(R.id.llProductSummary);
        container.removeAllViews();
        if (productMap.isEmpty()) {
            addEmptyText(container, "暂无销售数据");
            return;
        }
        for (Map.Entry<String, ProductSummary> entry : productMap.entrySet()) {
            ProductSummary summary = entry.getValue();
            LinearLayout row = createRow();
            TextView name = createText(entry.getKey(), 0xFF333333, 14, 1, false);
            TextView amount = createText(
                    String.format(Locale.getDefault(), "x%d  ¥%.2f", summary.quantity, summary.amount),
                    0xFFE53935, 14, 0, true);
            row.addView(name);
            row.addView(amount);
            container.addView(row);
            addDivider(container);
        }
    }

    private void bindOrderFlow(List<Order> orders) {
        LinearLayout container = findViewById(R.id.llOrderFlow);
        container.removeAllViews();
        if (orders.isEmpty()) {
            addEmptyText(container, "暂无订单流水");
            return;
        }
        for (Order order : orders) {
            double amount = presenter.getOrderPaidAmount(order) - order.refundAmount;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 8, 0, 8);

            LinearLayout top = createRow();
            top.addView(createText(order.name, 0xFF333333, 14, 1, false));
            int amountColor = order.refundAmount > 0 ? 0xFF8A8A8A : 0xFFE53935;
            top.addView(createText(String.format(Locale.getDefault(), "¥%.2f", Math.max(0, amount)),
                    amountColor, 14, 0, true));
            row.addView(top);

            TextView status = createText(statusText(order), statusColor(order), 12, 0, false);
            status.setPadding(10, 4, 10, 4);
            row.addView(status);

            row.addView(createInfoText("订单编号：" + order.orderId, 0xFF8A8F98));
            row.addView(createInfoText("到账时间：" + formatIncomeTime(order), 0xFF333333));
            row.addView(createInfoText("下单时间：" + (order.time == null ? "未记录" : order.time), 0xFF8A8F98));
            row.addView(createInfoText("买家：" + safeText(order.buyerNickname) + "  单价¥"
                    + String.format(Locale.getDefault(), "%.2f", order.unitPrice > 0 ? order.unitPrice : order.price)
                    + " x" + order.quantity, 0xFF8A8F98));
            row.addView(createInfoText("发货信息：" + shipmentText(order), 0xFF8A8F98));
            if (order.refundAmount > 0) {
                TextView refund = createText(
                        String.format(Locale.getDefault(), "已退款 ¥%.2f  原因：%s",
                                order.refundAmount,
                                order.refundReason == null || order.refundReason.isEmpty() ? "无" : order.refundReason),
                        0xFFE53935, 12, 1, false);
                refund.setPadding(0, 4, 0, 0);
                row.addView(refund);
            }
            container.addView(row);
            addDivider(container);
        }
    }

    private TextView createInfoText(String text, int color) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(12);
        tv.setLineSpacing(dp(2), 1.0f);
        tv.setPadding(0, 4, 0, 0);
        return tv;
    }

    private LinearLayout createRow() {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        return row;
    }

    private TextView createText(String text, int color, int sp, int weight, boolean end) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                weight > 0 ? 0 : LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.weight = weight;
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(sp);
        tv.setSingleLine(false);
        if (end)
            tv.setGravity(android.view.Gravity.END);
        return tv;
    }

    private void addDivider(LinearLayout container) {
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFF2F2F7);
        container.addView(divider);
    }

    private void addEmptyText(LinearLayout container, String text) {
        TextView tv = createText(text, 0xFF999999, 14, 0, false);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setMinHeight(dp(120));
        tv.setPadding(0, 36, 0, 36);
        container.addView(tv);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String formatIncomeTime(Order order) {
        if (order.completedAt > 0) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(order.completedAt));
        }
        return order.time == null || order.time.isEmpty() ? "未记录" : order.time;
    }

    private String shipmentText(Order order) {
        if (order.shipName == null || order.shipName.isEmpty()) {
            return "未记录";
        }
        String no = order.shipNo == null || order.shipNo.isEmpty() ? "未填写单号" : order.shipNo;
        if ("custom".equals(order.shipType)) {
            String phone = order.shipPhone == null || order.shipPhone.isEmpty() ? "未填写电话" : order.shipPhone;
            return order.shipName + " / " + no + " / " + phone;
        }
        return order.shipName + " / " + no;
    }

    private String safeText(String text) {
        return text == null || text.isEmpty() ? "未记录" : text;
    }

    private String scopeName(String scope) {
        if ("today".equals(scope))
            return "今日";
        if ("month".equals(scope))
            return "本月";
        return "累计";
    }

    private String scopeDesc(String scope) {
        if ("today".equals(scope))
            return "今日买家确认收货后的到账流水";
        if ("month".equals(scope))
            return "本月买家确认收货后的到账流水";
        return "累计买家确认收货后的到账流水";
    }

    private String statusText(Order order) {
        if (order.refundAmount > 0)
            return "已退款";
        String status = order.status;
        if (Order.STATUS_PAID.equals(status))
            return "待发货";
        if (Order.STATUS_SHIPPED.equals(status))
            return "已发货";
        if (Order.STATUS_COMPLETED.equals(status))
            return "已完成";
        return status;
    }

    private int statusColor(Order order) {
        if (order.refundAmount > 0)
            return 0xFFE53935;
        if (Order.STATUS_COMPLETED.equals(order.status))
            return 0xFF4CAF50;
        if (Order.STATUS_SHIPPED.equals(order.status))
            return 0xFF007AFF;
        return 0xFFFF9800;
    }
}
