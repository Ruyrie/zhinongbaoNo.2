package com.example.zhinongbao.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.Order;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    public interface OnItemClickListener { void onClick(Order order); }
    public interface OnActionListener {
        void onPay(Order order);
        void onCancel(Order order);
        void onReview(Order order);
        void onConfirmReceipt(Order order);
        void onRequestRefund(Order order);
    }

    private final List<Order> data;
    private OnItemClickListener clickListener;
    private OnActionListener actionListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean reviewMode = false;

    public OrderAdapter(List<Order> data) { this.data = data; }
    public void setOnItemClickListener(OnItemClickListener l) { this.clickListener = l; }
    public void setOnActionListener(OnActionListener l) { this.actionListener = l; }
    public void setReviewMode(boolean reviewMode) { this.reviewMode = reviewMode; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Order o = data.get(position);
        holder.tvName.setText(o.name);
        holder.tvPrice.setText(String.format("¥%.2f", o.getPayableAmount()));
        holder.tvQty.setText("x" + o.quantity);
        holder.tvTime.setText("下单：" + o.time);

        // 停止旧倒计时
        if (holder.countdownRunnable != null) {
            handler.removeCallbacks(holder.countdownRunnable);
            holder.countdownRunnable = null;
        }

        // 处理超时自动取消
        if (Order.STATUS_PENDING.equals(o.status) && o.getRemainingMs() <= 0) {
            o.status = Order.STATUS_CANCELLED;
        }

        if (reviewMode) {
            styleStatus(holder, "待评价", 0xFFFF9500, 0x1AFF9500);
            holder.tvCountdown.setText("交易成功");
            holder.tvCountdown.setTextColor(0xFF34C759);
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnPay.setText("去评价");
        } else {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setText("取消订单");
            holder.btnPay.setText("立即支付");
        }

        if (!reviewMode) switch (o.status) {
            case Order.STATUS_PENDING:
                styleStatus(holder, "待支付", 0xFFFF9500, 0x1AFF9500);
                holder.tvCountdown.setTextColor(0xFFFF9500);
                holder.layoutActions.setVisibility(View.VISIBLE);
                // 实时倒计时
                holder.countdownRunnable = new Runnable() {
                    @Override public void run() {
                        long rem = o.getRemainingMs();
                        if (rem <= 0) {
                            o.status = Order.STATUS_CANCELLED;
                            notifyItemChanged(holder.getAdapterPosition());
                            return;
                        }
                        long h = rem / 3600000, m = (rem % 3600000) / 60000, s = (rem % 60000) / 1000;
                        holder.tvCountdown.setText(String.format("剩余 %02d:%02d:%02d", h, m, s));
                        handler.postDelayed(this, 1000);
                    }
                };
                handler.post(holder.countdownRunnable);
                break;
            case Order.STATUS_PAID:
                styleStatus(holder, "待发货", 0xFF34C759, 0x1A34C759);
                holder.tvCountdown.setText("支付成功");
                holder.tvCountdown.setTextColor(0xFF34C759);
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnPay.setText("申请退款");
                break;
            case Order.STATUS_SHIPPED:
                styleStatus(holder, "待收货", 0xFF34C759, 0x1A34C759);
                holder.tvCountdown.setText("卖家已发货");
                holder.tvCountdown.setTextColor(0xFF34C759);
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setText("申请退款");
                holder.btnPay.setText("确认收货");
                break;
            case Order.STATUS_COMPLETED:
                styleStatus(holder, "已完成", 0xFF34C759, 0x1A34C759);
                holder.tvCountdown.setText("交易成功");
                holder.tvCountdown.setTextColor(0xFF34C759);
                holder.layoutActions.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.GONE);
                holder.btnPay.setText(isCompletedRefundWindowOpen(o) ? "申请退款" : "联系客服");
                if (o.refundAmount > 0) {
                    holder.layoutActions.setVisibility(View.GONE);
                    holder.tvCountdown.setText("已退款");
                }
                break;
            case Order.STATUS_REFUND:
                styleStatus(holder, "售后中", 0xFFFF9500, 0x1AFF9500);
                holder.tvCountdown.setText("售后处理中");
                holder.tvCountdown.setTextColor(0xFFFF9500);
                holder.layoutActions.setVisibility(View.GONE);
                break;
            case Order.STATUS_CANCELLED:
                styleStatus(holder, "已取消", 0xFF8A8A8A, 0x1A8A8A8A);
                holder.tvCountdown.setText("订单已取消");
                holder.tvCountdown.setTextColor(0xFF8A8A8A);
                holder.layoutActions.setVisibility(View.GONE);
                break;
        }

        holder.btnPay.setOnClickListener(v -> {
            if (actionListener == null)
                return;
            if (reviewMode) {
                actionListener.onReview(o);
            } else if (Order.STATUS_SHIPPED.equals(o.status)) {
                actionListener.onConfirmReceipt(o);
            } else if (Order.STATUS_PAID.equals(o.status) || Order.STATUS_COMPLETED.equals(o.status)) {
                actionListener.onRequestRefund(o);
            } else {
                actionListener.onPay(o);
            }
        });
        holder.btnCancel.setOnClickListener(v -> {
            if (actionListener == null)
                return;
            if (Order.STATUS_SHIPPED.equals(o.status)) {
                actionListener.onRequestRefund(o);
            } else {
                actionListener.onCancel(o);
            }
        });
        holder.itemView.setOnClickListener(v -> { if (clickListener != null) clickListener.onClick(o); });
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        if (holder.countdownRunnable != null) {
            handler.removeCallbacks(holder.countdownRunnable);
            holder.countdownRunnable = null;
        }
    }

    private void styleStatus(VH h, String text, int textColor, int bgColor) {
        h.tvStatus.setText(text);
        h.tvStatus.setTextColor(textColor);
        h.tvStatus.setBackgroundColor(bgColor);
        // set corner via padding only (no shape)
        h.tvStatus.setPadding(dp(h.itemView.getContext(), 8), dp(h.itemView.getContext(), 2),
                              dp(h.itemView.getContext(), 8), dp(h.itemView.getContext(), 2));
    }

    private int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    private boolean isCompletedRefundWindowOpen(Order order) {
        long base = order.completedAt > 0 ? order.completedAt : parseOrderTime(order.time);
        return base > 0 && System.currentTimeMillis() - base <= 7L * 24 * 60 * 60 * 1000;
    }

    private long parseOrderTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return 0;
        }
        try {
            java.util.Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(time);
            return date == null ? 0 : date.getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvQty, tvTime, tvCountdown, tvStatus;
        TextView btnPay, btnCancel;
        LinearLayout layoutActions;
        Runnable countdownRunnable;
        VH(View v) {
            super(v);
            tvName       = v.findViewById(R.id.tvOrderName);
            tvPrice      = v.findViewById(R.id.tvOrderPrice);
            tvQty        = v.findViewById(R.id.tvOrderQty);
            tvTime       = v.findViewById(R.id.tvOrderTime);
            tvCountdown  = v.findViewById(R.id.tvOrderCountdown);
            tvStatus     = v.findViewById(R.id.tvOrderStatus);
            layoutActions= v.findViewById(R.id.layoutOrderActions);
            btnPay       = v.findViewById(R.id.btnPayOrder);
            btnCancel    = v.findViewById(R.id.btnCancelOrder);
        }
    }
}
