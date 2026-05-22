package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.PurchaseRequestAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.mvp.purchasemarket.PurchaseMarketContract;
import com.example.zhinongbao.mvp.purchasemarket.PurchaseMarketPresenter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PurchaseMarketActivity extends BaseMvpActivity<PurchaseMarketContract.Presenter>
        implements PurchaseMarketContract.View {

    private String currentUser;
    private boolean isSeller;
    private RecyclerView rvRequests;
    private PurchaseRequestAdapter adapter;
    private List<PurchaseRequest> items;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_market);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        findViewById(R.id.ivPurchaseBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvToolbarPost).setOnClickListener(v ->
                startActivity(new Intent(this, PostPurchaseActivity.class)));

        rvRequests = findViewById(R.id.rvPurchaseRequests);
        tvEmpty = findViewById(R.id.tvPurchaseEmpty);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        new PurchaseMarketPresenter(this, this).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) presenter.refresh();
    }

    @Override
    public void showRequests(List<PurchaseRequest> newItems, String currentUser, boolean sellerMode) {
        this.currentUser = currentUser;
        this.isSeller = sellerMode;
        if (newItems.isEmpty()) {
            rvRequests.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvRequests.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }

        if (adapter == null) {
            items = newItems;
            adapter = new PurchaseRequestAdapter(items, currentUser, isSeller,
                    new PurchaseRequestAdapter.OnActionListener() {
                        @Override
                        public void onQuoteClick(PurchaseRequest req) {
                            presenter.onQuoteClick(req);
                        }
                        @Override
                        public void onViewQuotesClick(PurchaseRequest req) {
                            presenter.onViewQuotesClick(req);
                        }
                        @Override
                        public void onItemClick(PurchaseRequest req) {
                            presenter.onItemClick(req);
                        }
                    });
            rvRequests.setAdapter(adapter);
        } else {
            items.clear();
            items.addAll(newItems);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void showQuoteEditor(PurchaseRequest req) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quote, null);
        EditText etPrice = dialogView.findViewById(R.id.etQuotePrice);
        EditText etDesc = dialogView.findViewById(R.id.etQuoteDesc);
        TextView tvTotal = dialogView.findViewById(R.id.tvQuoteTotal);
        TextView tvReqInfo = dialogView.findViewById(R.id.tvQuoteReqInfo);
        tvReqInfo.setText(req.productName + " · " + formatQty(req.quantity) + req.unit
                + " · 目标价 ¥" + formatPrice(req.targetPrice));
        bindQuoteTotal(etPrice, tvTotal, req.quantity);

        new AlertDialog.Builder(this)
                .setTitle("报价")
                .setView(dialogView)
                .setPositiveButton("提交报价", (d, w) -> {
                    String priceStr = etPrice.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    presenter.submitQuote(req, priceStr, desc);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void showQuoteList(PurchaseRequest req, List<PurchaseQuote> quotes) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 ").append(req.productName)
          .append("  需求量：").append(formatQty(req.quantity)).append(req.unit)
          .append("  目标价：¥").append(formatPrice(req.targetPrice)).append("\n");
        if (req.description != null && !req.description.isEmpty()) {
            sb.append("备注：").append(req.description).append("\n");
        }
        sb.append("\n");

        if (quotes.isEmpty()) {
            sb.append("暂无商家报价");
        } else {
            sb.append("共 ").append(quotes.size()).append(" 个报价：\n\n");
            for (int i = 0; i < quotes.size(); i++) {
                PurchaseQuote q = quotes.get(i);
                sb.append(i + 1).append(". ").append(q.sellerNickname)
                  .append("  ¥").append(formatPrice(q.price)).append("/").append(req.unit).append("\n");
                if (q.description != null && !q.description.isEmpty()) {
                    sb.append("   ").append(q.description).append("\n");
                }
                sb.append("   ")
                  .append(new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(q.timestamp)))
                  .append("\n\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("采购详情 & 报价列表")
                .setMessage(sb.toString())
                .setPositiveButton("关闭", null)
                .show();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String formatQty(double qty) {
        return qty % 1 == 0 ? String.valueOf((int) qty) : String.valueOf(qty);
    }

    private String formatPrice(double price) {
        return price % 1 == 0 ? String.valueOf((int) price)
                : String.format(Locale.CHINA, "%.2f", price);
    }

    private void bindQuoteTotal(EditText etPrice, TextView tvTotal, double quantity) {
        etPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    double price = Double.parseDouble(s.toString());
                    tvTotal.setText(String.format(Locale.CHINA, "预计成交总额：¥%.2f", price * quantity));
                } catch (Exception e) {
                    tvTotal.setText("预计成交总额：¥0.00");
                }
            }
        });
    }
}
