package com.example.zhinongbao;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SellerPurchaseMgmtActivity extends AppCompatActivity {

    private TextView tabMarket, tabMyQuotes, tabMyRequests;
    private View tabIndicator;
    private RecyclerView rvPurchase;
    private DataManager dm;
    private String currentUser;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private int currentTab = 0; // 0=Market, 1=MyQuotes, 2=MyRequests

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_purchase_mgmt);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        dm = DataManager.getInstance(this);
        currentUser = dm.getLoggedUser();

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        tabMarket = findViewById(R.id.tabMarket);
        tabMyQuotes = findViewById(R.id.tabMyQuotes);
        tabMyRequests = findViewById(R.id.tabMyRequests);
        tabIndicator = findViewById(R.id.tabIndicator);
        rvPurchase = findViewById(R.id.rvPurchase);
        rvPurchase.setLayoutManager(new LinearLayoutManager(this));

        tabMarket.setOnClickListener(v -> switchTab(0));
        tabMyQuotes.setOnClickListener(v -> switchTab(1));
        tabMyRequests.setOnClickListener(v -> switchTab(2));

        tabIndicator.post(() -> switchTab(0));
    }

    private void switchTab(int tab) {
        currentTab = tab;
        tabMarket.setTextColor(tab == 0 ? 0xFF007AFF : 0xFF666666);
        tabMarket.setTypeface(null, tab == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabMyQuotes.setTextColor(tab == 1 ? 0xFF007AFF : 0xFF666666);
        tabMyQuotes.setTypeface(null, tab == 1 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabMyRequests.setTextColor(tab == 2 ? 0xFF007AFF : 0xFF666666);
        tabMyRequests.setTypeface(null, tab == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        TextView activeTab = tab == 0 ? tabMarket : (tab == 1 ? tabMyQuotes : tabMyRequests);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabIndicator.getLayoutParams();
        lp.width = activeTab.getWidth();
        lp.leftMargin = activeTab.getLeft();
        tabIndicator.setLayoutParams(lp);

        loadData();
    }

    private void loadData() {
        if (currentTab == 0) {
            List<PurchaseRequest> reqs = dm.getPurchaseRequests();
            // Filter out own requests
            reqs.removeIf(r -> r.buyerUser.equals(currentUser));
            rvPurchase.setAdapter(new MarketAdapter(reqs));
        } else if (currentTab == 1) {
            List<PurchaseQuote> quotes = dm.getQuotesBySellerUser(currentUser);
            rvPurchase.setAdapter(new MyQuotesAdapter(quotes));
        } else if (currentTab == 2) {
            List<PurchaseRequest> myReqs = dm.getMyPurchaseRequests(currentUser);
            rvPurchase.setAdapter(new MyRequestsAdapter(myReqs));
        }
    }

    private String formatTime(long ts) {
        return sdf.format(new Date(ts));
    }

    // ─── 市场采购 Adapter ────────────────────────────────────────────────────────
    class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.VH> {
        List<PurchaseRequest> list;

        MarketAdapter(List<PurchaseRequest> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_request, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PurchaseRequest r = list.get(position);
            holder.tvName.setText(r.productName);
            holder.tvUser.setText(r.buyerNickname);
            holder.tvCategory.setText(r.category);
            holder.tvTarget.setText("目标价: ¥" + r.targetPrice);
            holder.tvQuantity.setText("需求量: " + r.quantity + r.unit);
            holder.tvDesc.setText(r.description);
            holder.tvTime.setText(formatTime(r.timestamp));

            holder.btnQuote.setVisibility(View.VISIBLE);
            holder.btnQuote.setText("立即报价");
            holder.btnQuote.setBackgroundResource(R.drawable.bg_btn_outline_green);
            holder.btnQuote.setOnClickListener(v -> showQuoteDialog(r));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvUser, tvCategory, tvTarget, tvQuantity, tvDesc, tvTime, btnQuote;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvReqProductName);
                tvUser = v.findViewById(R.id.tvReqBuyer);
                tvCategory = v.findViewById(R.id.tvReqCategory);
                tvTarget = v.findViewById(R.id.tvReqTargetPrice);
                tvQuantity = v.findViewById(R.id.tvReqQuantity);
                tvDesc = v.findViewById(R.id.tvReqDesc);
                tvTime = v.findViewById(R.id.tvReqTime);
                btnQuote = v.findViewById(R.id.btnReqQuote);
            }
        }
    }

    private void showQuoteDialog(PurchaseRequest r) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_quote, null);
        EditText etPrice = v.findViewById(R.id.etQuotePrice);
        EditText etDesc = v.findViewById(R.id.etQuoteDesc);
        new AlertDialog.Builder(this)
                .setTitle("对 " + r.productName + " 报价")
                .setView(v)
                .setPositiveButton("提交", (d, w) -> {
                    try {
                        double p = Double.parseDouble(etPrice.getText().toString());
                        String desc = etDesc.getText().toString();
                        if (dm.addQuote(r.id, currentUser, p, desc)) {
                            Toast.makeText(this, "报价成功", Toast.LENGTH_SHORT).show();
                            loadData();
                        }
                    } catch (Exception e) {
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ─── 我的报价 Adapter ────────────────────────────────────────────────────────
    class MyQuotesAdapter extends RecyclerView.Adapter<MyQuotesAdapter.VH> {
        List<PurchaseQuote> list;

        MyQuotesAdapter(List<PurchaseQuote> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_quote, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PurchaseQuote q = list.get(position);
            holder.tvName.setText("需求商品: " + q.requestProductName);
            holder.tvPrice.setText("我的报价: ¥" + q.price);
            holder.tvDesc.setText("备注: " + q.description);
            holder.tvTime.setText(formatTime(q.timestamp));

            if ("accepted".equals(q.status)) {
                holder.tvStatus.setText("已接受");
                holder.tvStatus.setTextColor(0xFF4CAF50);
            } else if ("rejected".equals(q.status)) {
                holder.tvStatus.setText("已拒绝");
                holder.tvStatus.setTextColor(0xFFF44336);
            } else {
                holder.tvStatus.setText("待处理");
                holder.tvStatus.setTextColor(0xFFFF9800);
            }

            if (!TextUtils.isEmpty(q.replyDesc)) {
                holder.llReply.setVisibility(View.VISIBLE);
                holder.tvReply.setText(q.replyDesc);
            } else {
                holder.llReply.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus, tvPrice, tvDesc, tvTime, tvReply;
            LinearLayout llReply;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvProductName);
                tvStatus = v.findViewById(R.id.tvQuoteStatus);
                tvPrice = v.findViewById(R.id.tvQuotePrice);
                tvDesc = v.findViewById(R.id.tvQuoteDesc);
                tvTime = v.findViewById(R.id.tvQuoteTime);
                llReply = v.findViewById(R.id.llReplyContainer);
                tvReply = v.findViewById(R.id.tvReplyDesc);
            }
        }
    }

    // ─── 我的采购 Adapter ────────────────────────────────────────────────────────
    class MyRequestsAdapter extends RecyclerView.Adapter<MyRequestsAdapter.VH> {
        List<PurchaseRequest> list;

        MyRequestsAdapter(List<PurchaseRequest> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_request_seller, parent,
                    false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PurchaseRequest r = list.get(position);
            holder.tvName.setText(r.productName);
            holder.tvStatus.setText("收到 " + r.quoteCount + " 个报价");
            holder.tvQuantity.setText("需求量: " + r.quantity + r.unit);
            holder.tvTargetPrice.setText("目标价: ¥" + r.targetPrice);
            holder.tvDesc.setText("描述: " + r.description);
            holder.tvTime.setText(formatTime(r.timestamp));

            holder.llQuotes.removeAllViews();
            List<PurchaseQuote> quotes = dm.getQuotesForRequestWithStatus(r.id);
            for (PurchaseQuote q : quotes) {
                View qv = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_received_quote,
                        holder.llQuotes, false);
                TextView tvSeller = qv.findViewById(R.id.tvSellerName);
                TextView tvPrice = qv.findViewById(R.id.tvQuotePrice);
                TextView tvDesc = qv.findViewById(R.id.tvQuoteDesc);
                TextView tvStatus = qv.findViewById(R.id.tvQuoteStatus);
                TextView tvReply = qv.findViewById(R.id.tvReplyInfo);
                LinearLayout llActions = qv.findViewById(R.id.llActions);
                TextView btnAccept = qv.findViewById(R.id.btnAccept);
                TextView btnReject = qv.findViewById(R.id.btnReject);

                tvSeller.setText("卖家: " + q.sellerNickname);
                tvPrice.setText("报价: ¥" + q.price);
                tvDesc.setText("备注: " + q.description);

                if ("pending".equals(q.status)) {
                    llActions.setVisibility(View.VISIBLE);
                    tvStatus.setVisibility(View.GONE);
                    tvReply.setVisibility(View.GONE);

                    btnAccept.setOnClickListener(v -> showReplyDialog(q.id, true));
                    btnReject.setOnClickListener(v -> showReplyDialog(q.id, false));
                } else {
                    llActions.setVisibility(View.GONE);
                    tvStatus.setVisibility(View.VISIBLE);
                    if ("accepted".equals(q.status)) {
                        tvStatus.setText("状态：已同意");
                        tvStatus.setTextColor(0xFF4CAF50);
                    } else {
                        tvStatus.setText("状态：已拒绝");
                        tvStatus.setTextColor(0xFFF44336);
                    }
                    if (!TextUtils.isEmpty(q.replyDesc)) {
                        tvReply.setVisibility(View.VISIBLE);
                        tvReply.setText("我的回复: " + q.replyDesc);
                    }
                }
                holder.llQuotes.addView(qv);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus, tvQuantity, tvTargetPrice, tvDesc, tvTime;
            LinearLayout llQuotes;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvReqProductName);
                tvStatus = v.findViewById(R.id.tvReqStatus);
                tvQuantity = v.findViewById(R.id.tvReqQuantity);
                tvTargetPrice = v.findViewById(R.id.tvReqTargetPrice);
                tvDesc = v.findViewById(R.id.tvReqDesc);
                tvTime = v.findViewById(R.id.tvReqTime);
                llQuotes = v.findViewById(R.id.llQuotesContainer);
            }
        }
    }

    private void showReplyDialog(long quoteId, boolean isAccept) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_quote_reply, null);
        TextView tvInfo = v.findViewById(R.id.tvQuoteReplyInfo);
        EditText etReply = v.findViewById(R.id.etReplyDesc);

        tvInfo.setText(isAccept ? "确定同意此报价并生成采购订单吗？" : "确定拒绝此报价吗？");

        new AlertDialog.Builder(this)
                .setTitle(isAccept ? "同意报价" : "拒绝报价")
                .setView(v)
                .setPositiveButton("确定", (d, w) -> {
                    String reply = etReply.getText().toString();
                    if (isAccept) {
                        dm.acceptQuote(quoteId, reply);
                        Toast.makeText(this, "已接受报价并生成供货订单", Toast.LENGTH_SHORT).show();
                    } else {
                        dm.rejectQuote(quoteId, reply);
                        Toast.makeText(this, "已拒绝报价", Toast.LENGTH_SHORT).show();
                    }
                    loadData();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}