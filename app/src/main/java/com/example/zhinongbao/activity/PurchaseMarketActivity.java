package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import com.example.zhinongbao.utils.DialogUtils;
import java.text.NumberFormat;
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
    private AlertDialog quoteListDialog;

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
                        public void onEditClick(PurchaseRequest req) {
                            editPurchaseRequest(req);
                        }
                        @Override
                        public void onDeleteClick(PurchaseRequest req) {
                            confirmDeletePurchaseRequest(req);
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
        showQuoteEditor(req, null);
    }

    private void showQuoteEditor(PurchaseRequest req, PurchaseQuote quote) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quote, null);
        EditText etPrice = dialogView.findViewById(R.id.etQuotePrice);
        EditText etDesc = dialogView.findViewById(R.id.etQuoteDesc);
        TextView tvTotal = dialogView.findViewById(R.id.tvQuoteTotal);
        TextView tvReqInfo = dialogView.findViewById(R.id.tvQuoteReqInfo);
        tvReqInfo.setText(req.productName + " · " + formatQty(req.quantity) + req.unit
                + " · 买家预算 ¥" + formatPrice(req.targetPrice));
        if (quote != null) {
            if (quoteListDialog != null && quoteListDialog.isShowing()) {
                quoteListDialog.dismiss();
            }
            etPrice.setText(formatPrice(quote.price));
            etPrice.setSelection(etPrice.getText().length());
            etDesc.setText(quote.description == null ? "" : quote.description);
        }
        bindQuoteTotal(etPrice, tvTotal, req.quantity);

        DialogUtils.showContent(this, quote == null ? "提交采购报价" : "修改采购报价", null, dialogView,
                "取消", quote == null ? "提交报价" : "保存修改", false, () -> {
                    String priceStr = etPrice.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    presenter.submitQuote(req, priceStr, desc);
                    return true;
                });
    }

    @Override
    public void showQuoteList(PurchaseRequest req, List<PurchaseQuote> quotes) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackground(rounded(Color.WHITE, dp(22), Color.TRANSPARENT, 0));

        TextView title = createText("采购详情 & 报价列表", 21, 0xFF212529, true);
        root.addView(title);

        LinearLayout summary = createCard(0xFFF2FBF4, 0xFFCDEBD2);
        summary.addView(createText(req.productName, 18, 0xFF1F2D25, true));
        TextView meta = createText("购买量 " + formatQty(req.quantity) + req.unit
                + " · 买家预算 ¥" + formatPrice(req.targetPrice), 14, 0xFF4A6250, false);
        meta.setPadding(0, dp(8), 0, 0);
        summary.addView(meta);
        TextView total = createText("预算总价 ¥" + formatMoney(req.quantity * req.targetPrice),
                18, 0xFF2E7D32, true);
        total.setPadding(0, dp(10), 0, 0);
        summary.addView(total);
        if (!TextUtils.isEmpty(req.description)) {
            TextView desc = createText("备注：" + req.description, 14, 0xFF6C757D, false);
            desc.setPadding(0, dp(8), 0, 0);
            summary.addView(desc);
        }
        root.addView(summary);

        TextView count = createText(quotes.isEmpty() ? "暂无商家报价" : "共 " + quotes.size() + " 个报价",
                16, 0xFF212529, true);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        countLp.topMargin = dp(16);
        count.setLayoutParams(countLp);
        root.addView(count);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        if (quotes.isEmpty()) {
            TextView empty = createText("商家报价会显示在这里", 14, 0xFF9AA0A6, false);
            empty.setGravity(Gravity.CENTER);
            empty.setMinHeight(dp(104));
            list.addView(empty);
        } else {
            for (PurchaseQuote q : quotes) {
                list.addView(createQuoteCard(req, q));
            }
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.min(dp(320), getResources().getDisplayMetrics().heightPixels / 2));
        scrollLp.topMargin = dp(8);
        root.addView(scroll, scrollLp);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(root).create();
        quoteListDialog = dialog;
        TextView close = createButton("关闭", false);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        closeLp.topMargin = dp(18);
        root.addView(close, closeLp);

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = getResources().getDisplayMetrics().widthPixels - dp(40);
                window.setLayout(Math.min(width, dp(380)), LinearLayout.LayoutParams.WRAP_CONTENT);
            }
        });
        dialog.show();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void editPurchaseRequest(PurchaseRequest req) {
        if (!presenter.canModify(req)) {
            showToast("该采购需求已付款或状态变化，不能修改");
            return;
        }
        Intent intent = new Intent(this, PostPurchaseActivity.class);
        intent.putExtra("request_id", req.id);
        startActivity(intent);
    }

    private void confirmDeletePurchaseRequest(PurchaseRequest req) {
        if (!presenter.canModify(req)) {
            showToast("该采购需求已付款或状态变化，不能删除");
            return;
        }
        DialogUtils.showConfirm(this, "删除采购需求",
                "确认删除「" + req.productName + "」？相关商家报价也会一并清除。",
                "取消", "删除", true, () -> presenter.deleteRequest(req));
    }

    private String formatQty(double qty) {
        return qty % 1 == 0 ? String.valueOf((int) qty) : String.valueOf(qty);
    }

    private String formatPrice(double price) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.CHINA);
        format.setMaximumFractionDigits(price % 1 == 0 ? 0 : 2);
        format.setMinimumFractionDigits(0);
        return format.format(price);
    }

    private String formatMoney(double price) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.CHINA);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(price);
    }

    private void bindQuoteTotal(EditText etPrice, TextView tvTotal, double quantity) {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    double price = Double.parseDouble(cleanNumber(s.toString()));
                    tvTotal.setText("预计成交总额：¥" + formatMoney(price * quantity));
                } catch (Exception e) {
                    tvTotal.setText("预计成交总额：¥0.00");
                }
            }
        };
        etPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                watcher.afterTextChanged(s);
            }
        });
        watcher.afterTextChanged(etPrice.getText());
    }

    private View createQuoteCard(PurchaseRequest req, PurchaseQuote quote) {
        LinearLayout card = createCard(0xFFFFFFFF, 0xFFE9ECEF);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView seller = createText(quote.sellerNickname, 16, 0xFF212529, true);
        top.addView(seller, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView price = createText("¥" + formatPrice(quote.price) + "/" + req.unit, 17, 0xFFE53935, true);
        top.addView(price);
        card.addView(top);

        TextView quoteTotal = createText("报价总额 ¥" + formatMoney(quote.price * req.quantity), 14, 0xFF2E7D32, true);
        quoteTotal.setPadding(0, dp(8), 0, 0);
        card.addView(quoteTotal);

        if (!TextUtils.isEmpty(quote.description)) {
            TextView desc = createText(quote.description, 14, 0xFF6C757D, false);
            desc.setPadding(0, dp(8), 0, 0);
            card.addView(desc);
        }
        TextView time = createText(new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                .format(new Date(quote.timestamp)), 12, 0xFF9AA0A6, false);
        time.setPadding(0, dp(8), 0, 0);
        card.addView(time);

        if (currentUser != null && currentUser.equals(quote.sellerUser) && "pending".equals(quote.status)) {
            TextView edit = createButton("修改报价", true);
            edit.setOnClickListener(v -> showQuoteEditor(req, quote));
            LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
            editLp.topMargin = dp(12);
            card.addView(edit, editLp);
        }
        return card;
    }

    private LinearLayout createCard(int fill, int stroke) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(fill, dp(14), stroke, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createText(String text, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setLineSpacing(dp(2), 1f);
        return tv;
    }

    private TextView createButton(String text, boolean outline) {
        TextView button = createText(text, 15, outline ? 0xFF2E7D32 : Color.WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(outline ? 0xFFEFF8F0 : 0xFF4CAF50, dp(14),
                outline ? 0xFFC7E9CB : 0xFF4CAF50, 1));
        return button;
    }

    private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidthDp > 0) {
            drawable.setStroke(strokeWidthDp, strokeColor);
        }
        return drawable;
    }

    private String cleanNumber(String value) {
        return value == null ? "" : value.replace(",", "").replace("¥", "").trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
