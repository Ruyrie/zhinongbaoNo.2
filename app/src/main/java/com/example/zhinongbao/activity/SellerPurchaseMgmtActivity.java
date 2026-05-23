package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ImagePickerAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.mvp.sellerpurchase.SellerPurchaseContract;
import com.example.zhinongbao.mvp.sellerpurchase.SellerPurchasePresenter;
import com.example.zhinongbao.utils.DialogUtils;
import com.example.zhinongbao.utils.ImageUtils;
import java.io.File;
import java.util.ArrayList;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SellerPurchaseMgmtActivity extends BaseMvpActivity<SellerPurchaseContract.Presenter>
        implements SellerPurchaseContract.View {

    private TextView tabMarket, tabMyQuotes, tabMyRequests;
    private View tabIndicator;
    private RecyclerView rvPurchase;
    private String currentUser;
    private final List<Uri> quoteImageUris = new ArrayList<>();
    private ImagePickerAdapter quoteImageAdapter;
    private ActivityResultLauncher<String> pickQuoteImages;
    private ActivityResultLauncher<Uri> takeQuotePicture;
    private Uri currentQuoteCameraUri;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private int currentTab = 0; // 0=Market, 1=MyQuotes, 2=MyRequests

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_purchase_mgmt);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        pickQuoteImages = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            for (Uri uri : uris) {
                addQuoteImage(uri);
            }
        });
        takeQuotePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && currentQuoteCameraUri != null) {
                addQuoteImage(currentQuoteCameraUri);
            }
        });

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

        new SellerPurchasePresenter(this, this);
        currentUser = getSharedPreferences("pref_session", MODE_PRIVATE).getString("logged_user", "");
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

        presenter.switchTab(tab);
    }

    @Override
    public void showMarket(List<PurchaseRequest> requests) {
        rvPurchase.setAdapter(new MarketAdapter(requests));
    }

    @Override
    public void showMyQuotes(List<PurchaseQuote> quotes) {
        rvPurchase.setAdapter(new MyQuotesAdapter(quotes));
    }

    @Override
    public void showMyRequests(List<PurchaseRequest> requests) {
        rvPurchase.setAdapter(new MyRequestsAdapter(requests));
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
            holder.tvTarget.setText("¥" + formatPrice(r.targetPrice));
            holder.tvQuantity.setText(formatQuantity(r.quantity) + r.unit);
            holder.tvDesc.setText(r.description);
            holder.tvTime.setText(formatTime(r.timestamp));

            holder.btnQuote.setText(presenter.hasQuoted(r.id) ? "再次报价" : "立即报价");
            holder.btnQuote.setTextColor(0xFF2E7D32);
            holder.btnQuote.setBackgroundResource(R.drawable.bg_action_outline_green);
            if (r.buyerUser != null && r.buyerUser.equals(currentUser)) {
                holder.btnQuote.setVisibility(View.GONE);
                holder.btnViewQuotes.setVisibility(View.VISIBLE);
                holder.btnViewQuotes.setText("我的采购");
                holder.btnViewQuotes.setOnClickListener(v -> switchTab(2));
            } else {
                holder.btnQuote.setVisibility(View.VISIBLE);
                holder.btnQuote.setOnClickListener(v -> showQuoteDialog(r));
                holder.btnViewQuotes.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvUser, tvCategory, tvTarget, tvQuantity, tvDesc, tvTime, btnQuote, btnViewQuotes;

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
                btnViewQuotes = v.findViewById(R.id.btnViewQuotes);
            }
        }
    }

    private String formatQuantity(double quantity) {
        if (quantity % 1 == 0) {
            return String.valueOf((int) quantity);
        }
        return String.format(Locale.CHINA, "%.1f", quantity);
    }

    private String formatPrice(double price) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.CHINA);
        format.setMaximumFractionDigits(price % 1 == 0 ? 0 : 2);
        format.setMinimumFractionDigits(0);
        return format.format(price);
    }

    private void showQuoteDialog(PurchaseRequest r) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_quote, null);
        EditText etPrice = v.findViewById(R.id.etQuotePrice);
        EditText etDesc = v.findViewById(R.id.etQuoteDesc);
        TextView tvTotal = v.findViewById(R.id.tvQuoteTotal);
        TextView tvReqInfo = v.findViewById(R.id.tvQuoteReqInfo);
        RecyclerView rvImages = v.findViewById(R.id.rvQuoteImages);
        tvReqInfo.setText(r.category + " · " + formatQuantity(r.quantity) + r.unit
                + " · 买家预算 ¥" + formatPrice(r.targetPrice));
        bindQuoteImages(rvImages, null);
        bindQuoteTotal(etPrice, tvTotal, r.quantity);
        DialogUtils.showContent(this, "对 " + r.productName + " 报价", null, v,
                "取消", "提交", false, () -> {
                    try {
                        String desc = etDesc.getText().toString();
                        presenter.submitQuote(r, etPrice.getText().toString(), desc, joinQuoteImages());
                        return true;
                    } catch (Exception e) {
                        showToast("请填写有效报价");
                        return false;
                    }
                });
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
            holder.tvPrice.setText("我的报价: ¥" + formatPrice(q.price));
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

            holder.btnEditQuote.setVisibility("pending".equals(q.status) ? View.VISIBLE : View.GONE);
            holder.btnEditQuote.setOnClickListener(v -> showEditQuoteDialog(q));

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
            TextView tvName, tvStatus, tvPrice, tvDesc, tvTime, tvReply, btnEditQuote;
            LinearLayout llReply;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvProductName);
                tvStatus = v.findViewById(R.id.tvQuoteStatus);
                tvPrice = v.findViewById(R.id.tvQuotePrice);
                tvDesc = v.findViewById(R.id.tvQuoteDesc);
                tvTime = v.findViewById(R.id.tvQuoteTime);
                btnEditQuote = v.findViewById(R.id.btnEditQuote);
                llReply = v.findViewById(R.id.llReplyContainer);
                tvReply = v.findViewById(R.id.tvReplyDesc);
            }
        }
    }

    private void showEditQuoteDialog(PurchaseQuote quote) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_quote, null);
        EditText etPrice = v.findViewById(R.id.etQuotePrice);
        EditText etDesc = v.findViewById(R.id.etQuoteDesc);
        TextView tvTotal = v.findViewById(R.id.tvQuoteTotal);
        TextView tvReqInfo = v.findViewById(R.id.tvQuoteReqInfo);
        RecyclerView rvImages = v.findViewById(R.id.rvQuoteImages);
        tvReqInfo.setText("修改对「" + quote.requestProductName + "」的报价");
        tvTotal.setVisibility(View.GONE);
        etPrice.setText(formatPrice(quote.price));
        etPrice.setSelection(etPrice.getText().length());
        etDesc.setText(quote.description == null ? "" : quote.description);
        bindQuoteImages(rvImages, quote.images);

        DialogUtils.showContent(this, "修改已发布报价", null, v, "取消", "保存修改", false, () -> {
            PurchaseRequest request = new PurchaseRequest();
            request.id = quote.requestId;
            presenter.submitQuote(request, etPrice.getText().toString(), etDesc.getText().toString(), joinQuoteImages());
            return true;
        });
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
            holder.tvQuantity.setText("购买量: " + formatQuantity(r.quantity) + r.unit);
            holder.tvTargetPrice.setText("买家预算: ¥" + formatPrice(r.targetPrice));
            holder.tvDesc.setText("描述: " + r.description);
            holder.tvTime.setText(formatTime(r.timestamp));
            holder.btnEdit.setOnClickListener(v -> editPurchaseRequest(r));
            holder.btnDelete.setOnClickListener(v -> confirmDeletePurchaseRequest(r));

            holder.llQuotes.removeAllViews();
            List<PurchaseQuote> quotes = presenter.getQuotesForRequest(r.id);
            holder.btnViewQuotes.setText(quotes.isEmpty() ? "暂无报价" : "查看报价");
            holder.btnViewQuotes.setEnabled(!quotes.isEmpty());
            holder.btnViewQuotes.setAlpha(quotes.isEmpty() ? 0.55f : 1f);
            holder.btnViewQuotes.setOnClickListener(v -> {
                holder.llQuotes.setVisibility(holder.llQuotes.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                holder.btnViewQuotes.setText(holder.llQuotes.getVisibility() == View.VISIBLE ? "收起报价" : "查看报价");
            });
            holder.llQuotes.setVisibility(View.GONE);
            for (PurchaseQuote q : quotes) {
                View qv = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_received_quote,
                        holder.llQuotes, false);
                TextView tvSeller = qv.findViewById(R.id.tvSellerName);
                TextView tvPrice = qv.findViewById(R.id.tvQuotePrice);
                TextView tvDesc = qv.findViewById(R.id.tvQuoteDesc);
                TextView tvStatus = qv.findViewById(R.id.tvQuoteStatus);
                TextView tvReply = qv.findViewById(R.id.tvReplyInfo);
                HorizontalScrollView hsvImages = qv.findViewById(R.id.hsvQuoteImages);
                LinearLayout llImages = qv.findViewById(R.id.llQuoteImages);
                LinearLayout llActions = qv.findViewById(R.id.llActions);
                TextView btnAccept = qv.findViewById(R.id.btnAccept);
                TextView btnReject = qv.findViewById(R.id.btnReject);

                tvSeller.setText("卖家: " + q.sellerNickname);
                tvPrice.setText("报价: ¥" + q.price);
                tvDesc.setText("备注: " + q.description);
                bindQuotePreviews(hsvImages, llImages, q.images);

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
            TextView tvName, tvStatus, tvQuantity, tvTargetPrice, tvDesc, tvTime, btnEdit, btnDelete, btnViewQuotes;
            LinearLayout llQuotes;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvReqProductName);
                tvStatus = v.findViewById(R.id.tvReqStatus);
                tvQuantity = v.findViewById(R.id.tvReqQuantity);
                tvTargetPrice = v.findViewById(R.id.tvReqTargetPrice);
                tvDesc = v.findViewById(R.id.tvReqDesc);
                tvTime = v.findViewById(R.id.tvReqTime);
                btnEdit = v.findViewById(R.id.btnEditPurchase);
                btnDelete = v.findViewById(R.id.btnDeletePurchase);
                btnViewQuotes = v.findViewById(R.id.btnViewQuotes);
                llQuotes = v.findViewById(R.id.llQuotesContainer);
            }
        }
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

    private void showReplyDialog(long quoteId, boolean isAccept) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_quote_reply, null);
        TextView tvInfo = v.findViewById(R.id.tvQuoteReplyInfo);
        EditText etReply = v.findViewById(R.id.etReplyDesc);

        tvInfo.setText(isAccept
                ? "同意后将生成一笔待付款采购订单，请确认报价和收货地址无误。"
                : "拒绝后该商家的报价将不再进入采购订单。");

        DialogUtils.showContent(this, isAccept ? "同意报价" : "拒绝报价", null, v,
                "取消", isAccept ? "确定订单" : "确认拒绝", !isAccept, () -> {
                    String reply = etReply.getText().toString();
                    if (isAccept) {
                        presenter.acceptQuote(quoteId, reply);
                    } else {
                        presenter.rejectQuote(quoteId, reply);
                    }
                    return true;
                });
    }

    @Override
    public void promptAddAddress() {
        DialogUtils.showConfirm(this, "缺少收货地址",
                "同意报价前需要先设置默认收货地址，用于生成采购订单。",
                "取消", "去设置地址", false, () -> {
                    startActivity(new Intent(this, AddressManagerActivity.class));
                    return true;
                });
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    private void bindQuoteImages(RecyclerView rvImages, String images) {
        quoteImageUris.clear();
        if (!TextUtils.isEmpty(images)) {
            for (String uri : images.split(",")) {
                String trimmed = uri.trim();
                if (!trimmed.isEmpty()) {
                    quoteImageUris.add(Uri.parse(trimmed));
                }
            }
        }
        quoteImageAdapter = new ImagePickerAdapter(quoteImageUris, 9, new ImagePickerAdapter.OnImagePickerClickListener() {
            @Override
            public void onAddClick() {
                pickQuoteImageSource();
            }

            @Override
            public void onDeleteClick(int position) {
                quoteImageUris.remove(position);
                quoteImageAdapter.notifyDataSetChanged();
            }
        });
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(quoteImageAdapter);
    }

    private void pickQuoteImageSource() {
        if (quoteImageUris.size() >= 9) {
            showToast("最多上传 9 张照片");
            return;
        }
        ImageUtils.showImagePickerDialog(this, "添加商品图片", new ImageUtils.OnImagePickerListener() {
            @Override
            public void onTakePhoto() {
                currentQuoteCameraUri = createQuoteImageFile();
                takeQuotePicture.launch(currentQuoteCameraUri);
            }

            @Override
            public void onPickFromGallery() {
                pickQuoteImages.launch("image/*");
            }
        });
    }

    private void addQuoteImage(Uri uri) {
        if (uri == null) {
            return;
        }
        if (quoteImageUris.size() >= 9) {
            showToast("最多上传 9 张照片");
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
        quoteImageUris.add(uri);
        if (quoteImageAdapter != null) {
            quoteImageAdapter.notifyDataSetChanged();
        }
    }

    private Uri createQuoteImageFile() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists()) {
            imagePath.mkdirs();
        }
        File newFile = new File(imagePath, "quote_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
    }

    private String joinQuoteImages() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < quoteImageUris.size(); i++) {
            builder.append(quoteImageUris.get(i).toString());
            if (i < quoteImageUris.size() - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private void bindQuotePreviews(HorizontalScrollView scrollView, LinearLayout container, String images) {
        container.removeAllViews();
        if (TextUtils.isEmpty(images)) {
            scrollView.setVisibility(View.GONE);
            return;
        }
        for (String uriText : images.split(",")) {
            String trimmed = uriText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.bg_dialog_input);
            imageView.setImageURI(Uri.parse(trimmed));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(64));
            lp.setMarginEnd(dp(8));
            container.addView(imageView, lp);
        }
        scrollView.setVisibility(container.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
