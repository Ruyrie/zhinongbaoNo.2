package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.mvp.sellersales.SellerSalesContract;
import com.example.zhinongbao.mvp.sellersales.SellerSalesPresenter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ============================================================
 * 【卖家销售分析 / Seller Sales】View（Activity）
 * 卖家的销售统计页：按时间范围展示营收合计、订单流水(收入/退款)与按商品汇总。
 * 整体逻辑：onCreate 建 Presenter(带时间范围 scope)并 start()；Presenter 回调
 *   showSales(订单列表+合计)后，本类在内存里做二次汇总(按商品 ProductSummary、
 *   按时间 FlowRecord 收入/退款)并渲染图表式列表。营收用「净额」(退款生效才扣)。
 * 数据来源：本类不碰数据库，经 Presenter → OrderRepository。
 * 配合的文件：接口 SellerSalesContract；业务 SellerSalesPresenter；
 *   布局 activity_seller_sales_analysis.xml；模型 model/Order。
 * 在 MVP 中的位置：View 层。
 * 提示：在 IDE 里搜索「销售分析」可看本组相关文件。
 * ============================================================
 */
public class SellerSalesAnalysisActivity extends BaseMvpActivity<SellerSalesContract.Presenter>
        implements SellerSalesContract.View {

    private static class ProductSummary {
        int quantity;
        double amount;
    }

    private static class FlowRecord {
        static final int TYPE_INCOME = 0;
        static final int TYPE_REFUND = 1;

        final Order order;
        final int type;
        final long timestamp;

        FlowRecord(Order order, int type, long timestamp) {
            this.order = order;
            this.type = type;
            this.timestamp = timestamp;
        }
    }

    private static final int FLOW_PAGE_SIZE = 5;   // 订单流水每页条数
    private final List<FlowRecord> flowRecords = new java.util.ArrayList<>();
    private int flowPage = 0;

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
            double amount = presenter.getOrderNetRevenue(order);   // 净额：退款生效后才扣减

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
        ((TextView) findViewById(R.id.tvOrderCount)).setText(orders.size() + " 笔");
        double avg = orders.isEmpty() ? 0 : total / orders.size();
        ((TextView) findViewById(R.id.tvAvgAmount)).setText(String.format(Locale.getDefault(), "¥%.2f", avg));

        bindProductSummary(productMap);
        // 订单流水：先拆成独立流水记录，再按发生时间倒序分页，避免最新记录被挤到第二页。
        flowRecords.clear();
        for (Order order : orders) {
            long timestamp = flowTimestamp(order);
            flowRecords.add(new FlowRecord(order, FlowRecord.TYPE_INCOME, timestamp));
            if (!Order.STATUS_REFUND.equals(order.status) && order.refundAmount > 0) {
                flowRecords.add(new FlowRecord(order, FlowRecord.TYPE_REFUND, timestamp));
            }
        }
        Collections.sort(flowRecords, (left, right) -> {
            int timeCompare = Long.compare(right.timestamp, left.timestamp);
            if (timeCompare != 0) {
                return timeCompare;
            }
            return Integer.compare(right.type, left.type);
        });
        flowPage = 0;
        bindOrderFlow();
    }

    private void bindProductSummary(Map<String, ProductSummary> productMap) {
        LinearLayout container = findViewById(R.id.llProductSummary);
        container.removeAllViews();
        if (productMap.isEmpty()) {
            addEmptyText(container, "暂无销售数据");
            return;
        }
        int rank = 1;
        int index = 0;
        int size = productMap.size();
        for (Map.Entry<String, ProductSummary> entry : productMap.entrySet()) {
            ProductSummary summary = entry.getValue();
            LinearLayout row = createRow();

            row.addView(makeRankBadge(rank));

            TextView name = createText(entry.getKey(), 0xFF1F2329, 14, 1, false);
            ((LinearLayout.LayoutParams) name.getLayoutParams()).setMarginStart(dp(12));
            row.addView(name);

            row.addView(makeQtyPill("x" + summary.quantity));

            TextView amount = createText(
                    String.format(Locale.getDefault(), "¥%.2f", summary.amount),
                    0xFFE53935, 15, 0, true);
            ((LinearLayout.LayoutParams) amount.getLayoutParams()).setMarginStart(dp(10));
            amount.setTypeface(null, Typeface.BOLD);
            row.addView(amount);

            container.addView(row);
            if (index < size - 1) {
                addDivider(container);
            }
            rank++;
            index++;
        }
    }

    // 商品排名圆形徽标（1、2、3…）
    private TextView makeRankBadge(int rank) {
        TextView badge = new TextView(this);
        int boxSize = dp(24);
        badge.setLayoutParams(new LinearLayout.LayoutParams(boxSize, boxSize));
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setText(String.valueOf(rank));
        badge.setTextSize(12);
        badge.setTextColor(0xFF2E7D32);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setBackgroundResource(R.drawable.bg_sales_rank_badge);
        return badge;
    }

    // 销量胶囊（浅灰底）
    private TextView makeQtyPill(String text) {
        TextView pill = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(dp(8));
        pill.setLayoutParams(lp);
        pill.setText(text);
        pill.setTextSize(11);
        pill.setTextColor(0xFF6B7280);
        pill.setPadding(dp(7), dp(2), dp(7), dp(2));
        pill.setBackgroundResource(R.drawable.bg_sales_qty_pill);
        return pill;
    }

    // 分页渲染当前页的订单流水（按流水记录分页）
    private void bindOrderFlow() {
        LinearLayout container = findViewById(R.id.llOrderFlow);
        container.removeAllViews();
        if (flowRecords.isEmpty()) {
            addEmptyText(container, "暂无订单流水");
            return;
        }
        int totalPages = (flowRecords.size() + FLOW_PAGE_SIZE - 1) / FLOW_PAGE_SIZE;
        if (flowPage < 0) {
            flowPage = 0;
        }
        if (flowPage >= totalPages) {
            flowPage = totalPages - 1;
        }
        int start = flowPage * FLOW_PAGE_SIZE;
        int end = Math.min(start + FLOW_PAGE_SIZE, flowRecords.size());
        for (int i = start; i < end; i++) {
            FlowRecord record = flowRecords.get(i);
            if (record.type == FlowRecord.TYPE_REFUND) {
                addRefundRecord(container, record.order);
            } else {
                addIncomeRecord(container, record.order);
            }
        }
        if (totalPages > 1) {
            addPaginationBar(container, totalPages);
        }
    }

    // 底部分页栏：上一页 / 第 X/Y 页 / 下一页
    private void addPaginationBar(LinearLayout container, int totalPages) {
        LinearLayout bar = new LinearLayout(this);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(android.view.Gravity.CENTER);
        bar.setBackgroundResource(R.drawable.bg_ios_card);
        bar.setPadding(0, dp(12), 0, dp(12));

        TextView prev = makePageButton("上一页", flowPage > 0);
        prev.setOnClickListener(v -> {
            if (flowPage > 0) {
                flowPage--;
                bindOrderFlow();
            }
        });

        TextView indicator = new TextView(this);
        indicator.setText(String.format(Locale.getDefault(), "第 %d / %d 页", flowPage + 1, totalPages));
        indicator.setTextSize(13);
        indicator.setTextColor(0xFF333333);
        indicator.setPadding(dp(18), 0, dp(18), 0);

        TextView next = makePageButton("下一页", flowPage < totalPages - 1);
        next.setOnClickListener(v -> {
            if (flowPage < totalPages - 1) {
                flowPage++;
                bindOrderFlow();
            }
        });

        bar.addView(prev);
        bar.addView(indicator);
        bar.addView(next);
        container.addView(bar);
    }

    private TextView makePageButton(String text, boolean enabled) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(13);
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setPadding(dp(16), dp(6), dp(16), dp(6));
        btn.setTextColor(enabled ? 0xFF2E7D32 : 0xFFBBBBBB);
        btn.setBackgroundResource(R.drawable.bg_action_outline_green);
        btn.setEnabled(enabled);
        btn.setAlpha(enabled ? 1f : 0.5f);
        return btn;
    }

    // 收入记录：白底 + 左侧绿色条，金额用「+」前缀、绿色显示
    private void addIncomeRecord(LinearLayout container, Order order) {
        LinearLayout card = createFlowCard(true);

        LinearLayout top = createHeaderRow();
        top.addView(makeTypeTag("收入", 0xFF2E7D32));

        TextView name = createText(order.name, 0xFF1F2329, 14, 1, false);
        name.setTypeface(null, Typeface.BOLD);
        top.addView(name);

        top.addView(createText(String.format(Locale.getDefault(), "+¥%.2f", presenter.getOrderPaidAmount(order)),
                0xFF2E7D32, 16, 0, true));   // 绿色：收入（加项）
        card.addView(top);

        addStatusChip(card, statusText(order), statusColor(order));

        card.addView(createInfoText("订单编号：" + order.orderId, 0xFF8A8F98));
        card.addView(createInfoText("到账时间：" + formatIncomeTime(order), 0xFF4B5563));
        card.addView(createInfoText("下单时间：" + (order.time == null ? "未记录" : order.time), 0xFF8A8F98));
        card.addView(createInfoText("买家：" + safeText(order.buyerNickname) + "  单价¥"
                + String.format(Locale.getDefault(), "%.2f", order.unitPrice > 0 ? order.unitPrice : order.price)
                + " x" + order.quantity, 0xFF8A8F98));
        card.addView(createInfoText("发货信息：" + shipmentText(order), 0xFF8A8F98));

        // 售后处理中：退款尚未生效，营收暂不扣减，提示一句即可（不计为退款记录）
        if (Order.STATUS_REFUND.equals(order.status)) {
            String reason = order.refundReason == null || order.refundReason.isEmpty() ? "无" : order.refundReason;
            TextView pending = createText(String.format(Locale.getDefault(),
                    "售后处理中：买家申请退款 ¥%.2f（卖家同意或超 24 小时未处理后才扣减）  原因：%s",
                    order.refundAmount, reason), 0xFFFF9500, 12, 0, false);
            pending.setPadding(0, dp(6), 0, 0);
            card.addView(pending);
        }
        container.addView(card);
    }

    // 退款记录：白底 + 左侧红色条，金额用「−」前缀、红色显示（与收入记录分开成两条）
    private void addRefundRecord(LinearLayout container, Order order) {
        LinearLayout card = createFlowCard(false);

        LinearLayout top = createHeaderRow();
        top.addView(makeTypeTag("退款", 0xFFE53935));

        TextView name = createText(order.name, 0xFF1F2329, 14, 1, false);
        name.setTypeface(null, Typeface.BOLD);
        top.addView(name);

        top.addView(createText(String.format(Locale.getDefault(), "−¥%.2f", order.refundAmount),
                0xFFE53935, 16, 0, true));   // 红色：退款（减项）
        card.addView(top);

        String reason = order.refundReason == null || order.refundReason.isEmpty() ? "无" : order.refundReason;
        card.addView(createInfoText("订单编号：" + order.orderId, 0xFF8A8F98));
        card.addView(createInfoText("退款原因：" + reason, 0xFF8A8F98));
        card.addView(createInfoText("退款时间：" + formatIncomeTime(order), 0xFF8A8F98));
        card.addView(createInfoText(String.format(Locale.getDefault(),
                "该单实收 ¥%.2f", presenter.getOrderNetRevenue(order)), 0xFF8A8F98));
        container.addView(card);
    }

    // 流水卡容器：白底 + 左侧彩色强调条，卡片之间留间距
    private LinearLayout createFlowCard(boolean income) {
        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(income ? R.drawable.bg_flow_card_income : R.drawable.bg_flow_card_refund);
        card.setElevation(dp(1));
        // 左内边距加大，避开彩色强调条
        card.setPadding(dp(16), dp(13), dp(14), dp(13));
        return card;
    }

    // 流水卡顶部行（标签 + 名称 + 金额），垂直居中
    private LinearLayout createHeaderRow() {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    // 类型标签：收入（绿）/退款（红），白字实心圆角
    private TextView makeTypeTag(String text, int color) {
        TextView tag = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        tag.setLayoutParams(lp);
        tag.setText(text);
        tag.setTextSize(11);
        tag.setTextColor(0xFFFFFFFF);
        tag.setPadding(dp(7), dp(2), dp(7), dp(2));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(7));
        bg.setColor(color);
        tag.setBackground(bg);
        return tag;
    }

    // 状态胶囊：文字用状态色，背景为同色低透明度底
    private void addStatusChip(LinearLayout card, String text, int color) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        chip.setLayoutParams(lp);
        chip.setText(text);
        chip.setTextSize(11);
        chip.setTextColor(color);
        chip.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor((color & 0x00FFFFFF) | 0x1A000000);   // 约 10% 透明度的同色底
        chip.setBackground(bg);
        card.addView(chip);
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

    private long flowTimestamp(Order order) {
        if (order == null) {
            return 0;
        }
        if (order.completedAt > 0) {
            return order.completedAt;
        }
        if (order.time == null || order.time.isEmpty()) {
            return 0;
        }
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(order.time);
            return parsed == null ? 0 : parsed.getTime();
        } catch (Exception ignored) {
            return 0;
        }
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
            return "今日卖家到账销售流水（买家确认收货后到账）";
        if ("month".equals(scope))
            return "本月卖家到账销售流水（买家确认收货后到账）";
        return "累计卖家到账销售流水（买家确认收货后到账）";
    }

    private String statusText(Order order) {
        if (Order.STATUS_REFUND.equals(order.status))
            return "售后处理中";
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
        if (Order.STATUS_REFUND.equals(order.status))
            return 0xFFFF9500;          // 售后处理中
        if (order.refundAmount > 0)
            return 0xFFE53935;          // 已退款
        if (Order.STATUS_COMPLETED.equals(order.status))
            return 0xFF4CAF50;
        if (Order.STATUS_SHIPPED.equals(order.status))
            return 0xFF007AFF;
        return 0xFFFF9800;
    }
}
