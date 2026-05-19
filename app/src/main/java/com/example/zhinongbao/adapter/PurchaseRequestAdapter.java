package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.PurchaseRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PurchaseRequestAdapter extends RecyclerView.Adapter<PurchaseRequestAdapter.ViewHolder> {

    public interface OnActionListener {
        void onQuoteClick(PurchaseRequest req);
        void onViewQuotesClick(PurchaseRequest req);
        void onItemClick(PurchaseRequest req);
    }

    private final List<PurchaseRequest> items;
    private final String currentUser;
    private final boolean isSeller;
    private final OnActionListener listener;

    public PurchaseRequestAdapter(List<PurchaseRequest> items, String currentUser,
            boolean isSeller, OnActionListener listener) {
        this.items = items;
        this.currentUser = currentUser;
        this.isSeller = isSeller;
        this.listener = listener;
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
        String buyerLabel = req.buyerNickname;
        if (req.buyerUser.equals(currentUser)) buyerLabel += "（我）";
        h.tvBuyer.setText(buyerLabel);
        h.tvProductName.setText(req.productName);

        String qty = req.quantity % 1 == 0
                ? String.valueOf((int) req.quantity) : String.valueOf(req.quantity);
        h.tvQuantity.setText(qty + " " + req.unit);
        h.tvTargetPrice.setText("¥" + formatPrice(req.targetPrice) + "/" + req.unit);
        h.tvDesc.setText(req.description != null ? req.description : "");
        h.tvQuoteCount.setText(req.quoteCount > 0 ? "已有 " + req.quoteCount + " 个报价" : "暂无报价");
        h.tvTime.setText(formatTime(req.timestamp));

        // 卖家看到"我要报价"，买家（自己发的）看到"查看报价"
        if (isSeller && !req.buyerUser.equals(currentUser)) {
            h.btnQuote.setVisibility(View.VISIBLE);
            h.btnViewQuotes.setVisibility(View.GONE);
            h.btnQuote.setOnClickListener(v -> listener.onQuoteClick(req));
        } else if (req.buyerUser.equals(currentUser)) {
            h.btnQuote.setVisibility(View.GONE);
            h.btnViewQuotes.setVisibility(View.VISIBLE);
            h.btnViewQuotes.setOnClickListener(v -> listener.onViewQuotesClick(req));
        } else {
            h.btnQuote.setVisibility(View.GONE);
            h.btnViewQuotes.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onItemClick(req));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvBuyer, tvProductName, tvQuantity, tvTargetPrice,
                tvDesc, tvQuoteCount, tvTime, btnQuote, btnViewQuotes;

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
        }
    }

    private String formatPrice(double price) {
        if (price % 1 == 0) return String.valueOf((int) price);
        return String.format(Locale.CHINA, "%.2f", price);
    }

    private String formatTime(long ts) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(ts));
    }
}
