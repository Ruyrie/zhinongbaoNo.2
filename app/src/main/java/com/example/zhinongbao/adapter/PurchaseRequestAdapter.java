package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.PurchaseRequest;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ============================================================
 * 【采购市场-需求 / PurchaseRequest】Adapter（RecyclerView 适配器）
 * 整体逻辑：onBindViewHolder 填充卡片数据，并根据「当前用户身份」决定显示哪组按钮：
 *   可报价用户且非自己发布 → 显示「立即报价/再次报价」；自己发布的需求 → 显示
 *   「查看报价/编辑/删除」；其余情况隐藏按钮。若需求在 lockedRequestIds 中（已成交），
 *   报价按钮变灰显示「已成交」且不可点。各按钮点击均通过 OnActionListener 回调外部。
 * 关键概念：quotedRequestIds = 当前用户已报过价的需求 id；lockedRequestIds = 已付款成交
 *   被锁定的需求 id；两者由外部在数据刷新时更新进来。
 * 数据来源：构造时传入的 List<PurchaseRequest>（由 PurchaseMarketActivity 从 Presenter 拿到），
 *   本类不查数据库。
 * 配合的文件：数据模型 model/PurchaseRequest；行布局 res/layout/item_purchase_request.xml；
 *   使用方 PurchaseMarketActivity（实现按钮回调）。
 * MVP 数据流位置：本类属于 View 层，只负责画卡片并把操作回调上抛。
 * 提示：在 IDE 里搜索「采购」可看本组相关文件。
 * ============================================================
 */
public class PurchaseRequestAdapter extends RecyclerView.Adapter<PurchaseRequestAdapter.ViewHolder> {

    public interface OnActionListener {
        void onQuoteClick(PurchaseRequest req);
        void onViewQuotesClick(PurchaseRequest req);
        void onEditClick(PurchaseRequest req);
        void onDeleteClick(PurchaseRequest req);
        void onItemClick(PurchaseRequest req);
    }

    private final List<PurchaseRequest> items;
    private final String currentUser;
    private final boolean isSeller;
    private final Set<Long> quotedRequestIds;
    private final OnActionListener listener;

    public PurchaseRequestAdapter(List<PurchaseRequest> items, String currentUser,
            boolean isSeller, OnActionListener listener) {
        this(items, currentUser, isSeller, Collections.emptySet(), listener);
    }

    public PurchaseRequestAdapter(List<PurchaseRequest> items, String currentUser,
            boolean isSeller, Set<Long> quotedRequestIds, OnActionListener listener) {
        this.items = items;
        this.currentUser = currentUser;
        this.isSeller = isSeller;
        this.quotedRequestIds = quotedRequestIds == null ? new HashSet<>() : quotedRequestIds;
        this.listener = listener;
    }

    private final Set<Long> lockedRequestIds = new HashSet<>();

    public void updateQuotedRequestIds(Set<Long> ids) {
        quotedRequestIds.clear();
        if (ids != null) {
            quotedRequestIds.addAll(ids);
        }
    }

    // 已成交（买家已付款且未退款）的需求 id，报价按钮将被锁定
    public void updateLockedRequestIds(Set<Long> ids) {
        lockedRequestIds.clear();
        if (ids != null) {
            lockedRequestIds.addAll(ids);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_purchase_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        PurchaseRequest req = items.get(position);

        h.tvCategory.setText(req.category != null && !req.category.isEmpty() ? req.category : "农产品");
        boolean ownRequest = req.buyerUser != null && req.buyerUser.equals(currentUser);
        String buyerLabel = req.buyerNickname;
        if (ownRequest) buyerLabel += "（我）";
        h.tvBuyer.setText(buyerLabel);
        h.tvProductName.setText(req.productName);

        String qty = req.quantity % 1 == 0
                ? String.valueOf((int) req.quantity) : String.valueOf(req.quantity);
        h.tvQuantity.setText(qty + " " + req.unit);
        h.tvTargetPrice.setText("¥" + formatPrice(req.targetPrice) + "/" + req.unit);
        h.tvDesc.setText(req.description != null ? req.description : "");
        h.tvQuoteCount.setText(req.quoteCount > 0 ? "已有 " + req.quoteCount + " 个报价" : "暂无报价");
        h.tvTime.setText(formatTime(req.timestamp));

        // 可报价用户看到"我要报价"，自己发布的需求看到"查看报价"
        if (isSeller && !ownRequest) {
            h.btnQuote.setVisibility(View.VISIBLE);
            h.btnViewQuotes.setVisibility(View.GONE);
            h.btnEdit.setVisibility(View.GONE);
            h.btnDelete.setVisibility(View.GONE);
            // 已成交（买家已付款且未退款）→ 锁定报价；退款/取消后才可再次报价
            if (lockedRequestIds.contains(req.id)) {
                h.btnQuote.setText("已成交");
                h.btnQuote.setEnabled(false);
                h.btnQuote.setAlpha(0.5f);
                h.btnQuote.setOnClickListener(null);
            } else {
                h.btnQuote.setText(quotedRequestIds.contains(req.id) ? "再次报价" : "立即报价");
                h.btnQuote.setEnabled(true);
                h.btnQuote.setAlpha(1f);
                h.btnQuote.setOnClickListener(v -> listener.onQuoteClick(req));
            }
        } else if (ownRequest) {
            h.btnQuote.setVisibility(View.GONE);
            h.btnViewQuotes.setVisibility(View.VISIBLE);
            h.btnEdit.setVisibility(View.VISIBLE);
            h.btnDelete.setVisibility(View.VISIBLE);
            h.btnViewQuotes.setOnClickListener(v -> listener.onViewQuotesClick(req));
            h.btnEdit.setOnClickListener(v -> listener.onEditClick(req));
            h.btnDelete.setOnClickListener(v -> listener.onDeleteClick(req));
        } else {
            h.btnQuote.setVisibility(View.GONE);
            h.btnViewQuotes.setVisibility(View.GONE);
            h.btnEdit.setVisibility(View.GONE);
            h.btnDelete.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onItemClick(req));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvBuyer, tvProductName, tvQuantity, tvTargetPrice,
                tvDesc, tvQuoteCount, tvTime, btnQuote, btnViewQuotes, btnEdit, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvCategory = v.findViewById(R.id.tvReqCategory);
            tvBuyer = v.findViewById(R.id.tvReqBuyer);
            tvProductName = v.findViewById(R.id.tvReqProductName);
            tvQuantity = v.findViewById(R.id.tvReqQuantity);
            tvTargetPrice = v.findViewById(R.id.tvReqTargetPrice);
            tvDesc = v.findViewById(R.id.tvReqDesc);
            tvQuoteCount = v.findViewById(R.id.tvReqQuoteCount);
            tvTime = v.findViewById(R.id.tvReqTime);
            btnQuote = v.findViewById(R.id.btnReqQuote);
            btnViewQuotes = v.findViewById(R.id.btnViewQuotes);
            btnEdit = v.findViewById(R.id.btnEditPurchase);
            btnDelete = v.findViewById(R.id.btnDeletePurchase);
        }
    }

    private String formatPrice(double price) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.CHINA);
        format.setMaximumFractionDigits(price % 1 == 0 ? 0 : 2);
        format.setMinimumFractionDigits(0);
        return format.format(price);
    }

    private String formatTime(long ts) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(ts));
    }
}
