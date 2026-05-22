package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.mvp.sellerstore.SellerStoreContract;
import com.example.zhinongbao.mvp.sellerstore.SellerStorePresenter;
import java.util.ArrayList;
import java.util.List;

public class SellerStoreActivity extends BaseMvpActivity<SellerStoreContract.Presenter> implements SellerStoreContract.View {

    private String seller;
    private boolean isOwnStore;
    private final List<Product> products = new ArrayList<>();
    private StoreProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_store);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        new SellerStorePresenter(this, this);
        seller = getIntent().getStringExtra("seller");
        if (seller == null || seller.isEmpty()) seller = presenter.getCurrentUser();

        // 返回
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        TextView tvAddProduct = findViewById(R.id.tvAddProduct);
        tvAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        TextView tvEditStoreInfo = findViewById(R.id.tvEditStoreInfo);
        tvEditStoreInfo.setOnClickListener(v -> showEditStoreDialog());

        // 列表
        RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StoreProductAdapter(products, isOwnStore, product -> {
            // 点击商品→详情
            Intent i = new Intent(this, ProductDetailActivity.class);
            i.putExtra("product_id", product.id);
            startActivity(i);
        }, product -> {
            // 下架
            new AlertDialog.Builder(this)
                    .setTitle("下架商品")
                    .setMessage("确认将\"" + product.name + "\"下架？下架后买家将无法购买。")
                    .setPositiveButton("确认下架", (d, w) -> {
                        presenter.deleteProduct(product.id);
                        Toast.makeText(this, "已下架", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }, presenter::getProductOrderCount);
        rv.setAdapter(adapter);
        presenter.loadStore(seller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null && seller != null) {
            presenter.loadStore(seller);
        }
    }

    @Override
    public void showStoreMeta(String seller, String storeName, String storePhone, boolean ownStore) {
        this.seller = seller;
        this.isOwnStore = ownStore;
        findViewById(R.id.tvAddProduct).setVisibility(ownStore ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvEditStoreInfo).setVisibility(ownStore ? View.VISIBLE : View.GONE);
        findViewById(R.id.llSalesStats).setVisibility(ownStore ? View.VISIBLE : View.GONE);
        if (adapter != null) {
            adapter.setOwnStore(ownStore);
        }
        ((TextView) findViewById(R.id.tvStoreName)).setText(ownStore ? "我的店铺" : storeName);
        ((TextView) findViewById(R.id.tvStoreDisplayName)).setText(storeName);
        String phone = storePhone;
        ((TextView) findViewById(R.id.tvStorePhone)).setText(
                phone == null || phone.isEmpty() ? "电话：未填写" : "电话：" + phone);
    }

    private void showEditStoreDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        EditText etName = new EditText(this);
        etName.setHint("店铺名称");
        etName.setText(((TextView) findViewById(R.id.tvStoreDisplayName)).getText());
        box.addView(etName);

        EditText etPhone = new EditText(this);
        etPhone.setHint("商铺电话");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        String phoneText = ((TextView) findViewById(R.id.tvStorePhone)).getText().toString().replace("电话：", "");
        etPhone.setText("未填写".equals(phoneText) ? "" : phoneText);
        box.addView(etPhone);

        new AlertDialog.Builder(this)
                .setTitle("修改店铺信息")
                .setView(box)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this, "店铺名称和电话不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    presenter.updateStoreInfo(name, phone);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void showProducts(List<Product> fresh) {
        products.clear();
        products.addAll(fresh);
        if (adapter != null) adapter.notifyDataSetChanged();

        RecyclerView rv = findViewById(R.id.rvProducts);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        boolean empty = products.isEmpty();
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showStats(int totalOrders, double totalRevenue) {
        ((TextView) findViewById(R.id.tvTotalOrders)).setText(String.valueOf(totalOrders));
        ((TextView) findViewById(R.id.tvTotalRevenue)).setText(
                String.format("¥%.2f", totalRevenue));
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ── 内部 Adapter ──

    interface OnProductClick { void onClick(Product p); }

    static class StoreProductAdapter extends RecyclerView.Adapter<StoreProductAdapter.VH> {
        private final List<Product> items;
        private boolean isOwn;
        private final OnProductClick clickListener;
        private final OnProductClick delistListener;
        private final ProductSalesResolver salesResolver;

        interface ProductSalesResolver {
            int getProductOrderCount(int productId);
        }

        StoreProductAdapter(List<Product> items, boolean isOwn,
                OnProductClick click, OnProductClick delist) {
            this(items, isOwn, click, delist, productId -> 0);
        }

        StoreProductAdapter(List<Product> items, boolean isOwn,
                OnProductClick click, OnProductClick delist, ProductSalesResolver salesResolver) {
            this.items = items;
            this.isOwn = isOwn;
            this.clickListener = click;
            this.delistListener = delist;
            this.salesResolver = salesResolver;
        }

        void setOwnStore(boolean ownStore) {
            this.isOwn = ownStore;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_seller_product, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Product p = items.get(position);
            h.tvName.setText(p.name);
            h.tvPrice.setText(String.format("¥%.2f", p.price));

            int orderCount = salesResolver.getProductOrderCount(p.id);
            h.tvSales.setText("已售 " + orderCount + " 件");

            // 封面图
            int defaultRes = getDefaultProductImageRes(p.id);
            if (defaultRes != 0) {
                h.ivCover.setImageResource(defaultRes);
            } else if (p.coverUri != null && !p.coverUri.isEmpty()) {
                String firstUri = p.coverUri.contains(",")
                        ? p.coverUri.split(",")[0] : p.coverUri;
                try {
                    if (firstUri.startsWith("data:image")) {
                        com.example.zhinongbao.utils.ImageUtils
                                .setAvatarFromBase64(h.ivCover, firstUri);
                    } else {
                        h.ivCover.setImageURI(Uri.parse(firstUri));
                    }
                } catch (Exception ignored) {
                    h.ivCover.setImageResource(R.drawable.ic_product_placeholder);
                }
            } else {
                h.ivCover.setImageResource(R.drawable.ic_product_placeholder);
            }

            // 下架按钮
            if (isOwn) {
                h.btnDelist.setVisibility(View.VISIBLE);
                h.btnDelist.setOnClickListener(v -> delistListener.onClick(p));
            } else {
                h.btnDelist.setVisibility(View.GONE);
            }

            h.itemView.setOnClickListener(v -> clickListener.onClick(p));
        }

        private int getDefaultProductImageRes(int productId) {
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

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvName, tvPrice, tvSales, btnDelist;

            VH(View v) {
                super(v);
                ivCover   = v.findViewById(R.id.ivProductCover);
                tvName    = v.findViewById(R.id.tvProductName);
                tvPrice   = v.findViewById(R.id.tvProductPrice);
                tvSales   = v.findViewById(R.id.tvSalesCount);
                btnDelist = v.findViewById(R.id.btnDelist);
            }
        }
    }
}
