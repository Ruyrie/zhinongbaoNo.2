package com.example.zhinongbao;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Product;
import java.util.ArrayList;
import java.util.List;

public class SellerStoreActivity extends AppCompatActivity {

    private DataManager dm;
    private String seller;
    private String currentUser;
    private boolean isOwnStore;
    private final List<Product> products = new ArrayList<>();
    private StoreProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_store);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dm = DataManager.getInstance(this);
        currentUser = dm.getLoggedUser();
        seller = getIntent().getStringExtra("seller");
        if (seller == null || seller.isEmpty()) seller = currentUser;
        isOwnStore = seller.equals(currentUser);

        // 标题
        String nick = dm.getNickname(seller);
        TextView tvStoreName = findViewById(R.id.tvStoreName);
        tvStoreName.setText(isOwnStore ? "我的店铺" : nick + "的店铺");

        // 返回
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // 上架按钮（仅自己）
        TextView tvAddProduct = findViewById(R.id.tvAddProduct);
        if (isOwnStore) {
            tvAddProduct.setVisibility(View.VISIBLE);
            tvAddProduct.setOnClickListener(v ->
                    startActivity(new Intent(this, AddProductActivity.class)));
        }

        // 销售统计（仅自己）
        View llSalesStats = findViewById(R.id.llSalesStats);
        if (isOwnStore) {
            llSalesStats.setVisibility(View.VISIBLE);
            refreshStats();
        }

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
                        dm.deleteProduct(product.id);
                        loadProducts();
                        if (isOwnStore) refreshStats();
                        Toast.makeText(this, "已下架", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
        if (isOwnStore) refreshStats();
    }

    private void loadProducts() {
        List<Product> fresh = dm.getProductsBySeller(seller);
        products.clear();
        products.addAll(fresh);
        if (adapter != null) adapter.notifyDataSetChanged();

        RecyclerView rv = findViewById(R.id.rvProducts);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        boolean empty = products.isEmpty();
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void refreshStats() {
        int totalOrders = dm.getTotalOrderCountForSeller(seller);
        double totalRevenue = dm.getTotalRevenueForSeller(seller);
        ((TextView) findViewById(R.id.tvTotalOrders)).setText(String.valueOf(totalOrders));
        ((TextView) findViewById(R.id.tvTotalRevenue)).setText(
                String.format("¥%.2f", totalRevenue));
    }

    // ── 内部 Adapter ──

    interface OnProductClick { void onClick(Product p); }

    static class StoreProductAdapter extends RecyclerView.Adapter<StoreProductAdapter.VH> {
        private final List<Product> items;
        private final boolean isOwn;
        private final OnProductClick clickListener;
        private final OnProductClick delistListener;

        StoreProductAdapter(List<Product> items, boolean isOwn,
                OnProductClick click, OnProductClick delist) {
            this.items = items;
            this.isOwn = isOwn;
            this.clickListener = click;
            this.delistListener = delist;
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

            DataManager dm = DataManager.getInstance(h.itemView.getContext());
            int orderCount = dm.getProductOrderCount(p.id);
            h.tvSales.setText("已售 " + orderCount + " 件");

            // 封面图
            if (p.coverUri != null && !p.coverUri.isEmpty()) {
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
                    h.ivCover.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                h.ivCover.setImageResource(android.R.drawable.ic_menu_gallery);
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
