package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.StoreFootprint;
import com.example.zhinongbao.mvp.footprint.FootprintContract;
import com.example.zhinongbao.mvp.footprint.FootprintPresenter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FootprintActivity extends BaseMvpActivity<FootprintContract.Presenter> implements FootprintContract.View {
    private final List<Row> rows = new ArrayList<>();
    private FootprintAdapter adapter;
    private boolean storeMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_footprint);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        findViewById(R.id.tvFootprintBack).setOnClickListener(v -> finish());
        findViewById(R.id.tabFootprintProducts).setOnClickListener(v -> {
            storeMode = false;
            loadRows();
        });
        findViewById(R.id.tabFootprintStores).setOnClickListener(v -> {
            storeMode = true;
            loadRows();
        });

        RecyclerView rv = findViewById(R.id.rvFootprints);
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return rows.get(position).type == Row.TYPE_PRODUCT ? 1 : 3;
            }
        });
        rv.setLayoutManager(glm);
        adapter = new FootprintAdapter();
        rv.setAdapter(adapter);
        new FootprintPresenter(this, this).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRows();
    }

    private void loadRows() {
        if (presenter == null) {
            return;
        }
        if (storeMode) {
            presenter.loadStores();
        } else {
            presenter.loadProducts();
        }
    }

    @Override
    public void showProductFootprints(List<Product> products) {
        rows.clear();
        String lastDate = "";
        for (Product product : products) {
            String date = formatDate(product.viewedAt);
            if (!date.equals(lastDate)) {
                rows.add(Row.date(date));
                lastDate = date;
            }
            rows.add(Row.product(product));
        }
        updateTitle(products.size());
        renderRows();
    }

    @Override
    public void showStoreFootprints(List<StoreFootprint> stores) {
        rows.clear();
        String lastDate = "";
        for (StoreFootprint store : stores) {
            String date = formatDate(store.viewedAt);
            if (!date.equals(lastDate)) {
                rows.add(Row.date(date));
                lastDate = date;
            }
            rows.add(Row.store(store));
        }
        updateTitle(stores.size());
        renderRows();
    }

    private void renderRows() {
        updateTabs();
        adapter.notifyDataSetChanged();
        boolean empty = rows.isEmpty();
        findViewById(R.id.rvFootprints).setVisibility(empty ? View.GONE : View.VISIBLE);
        TextView emptyView = findViewById(R.id.tvFootprintEmpty);
        emptyView.setText(storeMode ? "暂无浏览店铺" : "暂无浏览商品");
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void updateTitle(int count) {
        ((TextView) findViewById(R.id.tvFootprintTitle)).setText("我的足迹 (" + count + ")");
    }

    private void updateTabs() {
        TextView productTab = findViewById(R.id.tabFootprintProducts);
        TextView storeTab = findViewById(R.id.tabFootprintStores);
        productTab.setTextColor(storeMode ? 0xFF8A8A8A : 0xFF1F1F1F);
        storeTab.setTextColor(storeMode ? 0xFF1F1F1F : 0xFF8A8A8A);
        productTab.setTypeface(null, storeMode ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        storeTab.setTypeface(null, storeMode ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private String formatDate(long millis) {
        if (millis <= 0) return "更早";
        return new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(millis));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class Row {
        static final int TYPE_DATE = 0;
        static final int TYPE_PRODUCT = 1;
        static final int TYPE_STORE = 2;
        int type;
        String date;
        Product product;
        StoreFootprint store;

        static Row date(String date) {
            Row row = new Row();
            row.type = TYPE_DATE;
            row.date = date;
            return row;
        }

        static Row product(Product product) {
            Row row = new Row();
            row.type = TYPE_PRODUCT;
            row.product = product;
            return row;
        }

        static Row store(StoreFootprint store) {
            Row row = new Row();
            row.type = TYPE_STORE;
            row.store = store;
            return row;
        }
    }

    private class FootprintAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return rows.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == Row.TYPE_DATE) {
                TextView tv = new TextView(parent.getContext());
                tv.setTextColor(0xFF1F1F1F);
                tv.setTextSize(18);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setGravity(Gravity.CENTER_VERTICAL);
                tv.setPadding(dp(12), dp(18), dp(12), dp(8));
                return new RecyclerView.ViewHolder(tv) {};
            }
            if (viewType == Row.TYPE_STORE) {
                LinearLayout box = new LinearLayout(parent.getContext());
                box.setOrientation(LinearLayout.HORIZONTAL);
                box.setBackgroundColor(Color.WHITE);
                box.setPadding(dp(14), dp(12), dp(14), dp(12));
                box.setGravity(Gravity.CENTER_VERTICAL);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(dp(10), dp(5), dp(10), dp(7));
                box.setLayoutParams(lp);

                FrameLayout avatarWrap = new FrameLayout(parent.getContext());
                ImageView avatar = new ImageView(parent.getContext());
                avatar.setId(3);
                avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                avatar.setBackgroundResource(R.drawable.bg_auth_logo);
                avatar.setVisibility(View.GONE);
                TextView placeholder = new TextView(parent.getContext());
                placeholder.setId(4);
                placeholder.setBackgroundResource(R.drawable.bg_auth_logo);
                placeholder.setGravity(Gravity.CENTER);
                placeholder.setTextColor(Color.WHITE);
                placeholder.setTextSize(16);
                placeholder.setTypeface(null, android.graphics.Typeface.BOLD);
                avatarWrap.addView(avatar, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                avatarWrap.addView(placeholder, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(48), dp(48));
                avatarLp.setMargins(0, 0, dp(12), 0);
                box.addView(avatarWrap, avatarLp);

                LinearLayout info = new LinearLayout(parent.getContext());
                info.setOrientation(LinearLayout.VERTICAL);
                TextView name = new TextView(parent.getContext());
                name.setId(1);
                name.setTextColor(0xFF1F1F1F);
                name.setTextSize(16);
                name.setTypeface(null, android.graphics.Typeface.BOLD);
                TextView sub = new TextView(parent.getContext());
                sub.setId(2);
                sub.setTextColor(0xFF8A8A8A);
                sub.setTextSize(13);
                sub.setPadding(0, dp(4), 0, 0);
                info.addView(name);
                info.addView(sub);
                box.addView(info, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                TextView enter = new TextView(parent.getContext());
                enter.setId(5);
                enter.setText("进店逛逛");
                enter.setTextColor(0xFF43A047);
                enter.setTextSize(14);
                enter.setTypeface(null, android.graphics.Typeface.BOLD);
                enter.setGravity(Gravity.CENTER);
                enter.setPadding(dp(10), dp(6), 0, dp(6));
                box.addView(enter, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new RecyclerView.ViewHolder(box) {};
            }

            LinearLayout box = new LinearLayout(parent.getContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(5), dp(5), dp(5), dp(6));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(3), dp(3), dp(3), dp(6));
            box.setLayoutParams(lp);
            ImageView image = new ImageView(parent.getContext());
            image.setId(1);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFFE8F5E9);
            box.addView(image, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(112)));
            TextView name = new TextView(parent.getContext());
            name.setId(2);
            name.setTextColor(0xFF333333);
            name.setTextSize(12);
            name.setMaxLines(1);
            name.setPadding(0, dp(5), 0, 0);
            box.addView(name);
            TextView price = new TextView(parent.getContext());
            price.setId(3);
            price.setTextColor(0xFFF04142);
            price.setTextSize(14);
            price.setTypeface(null, android.graphics.Typeface.BOLD);
            box.addView(price);
            return new RecyclerView.ViewHolder(box) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            if (row.type == Row.TYPE_DATE) {
                ((TextView) holder.itemView).setText(row.date);
                return;
            }
            if (row.type == Row.TYPE_STORE) {
                TextView name = holder.itemView.findViewById(1);
                TextView sub = holder.itemView.findViewById(2);
                ImageView avatar = holder.itemView.findViewById(3);
                TextView placeholder = holder.itemView.findViewById(4);
                TextView enter = holder.itemView.findViewById(5);
                name.setText(row.store.storeName);
                String phone = row.store.storePhone == null || row.store.storePhone.isEmpty()
                        ? "未填写电话" : row.store.storePhone;
                sub.setText("商品 " + row.store.productCount + " 件 · 电话 " + phone);
                bindStoreAvatar(avatar, placeholder, row.store);
                View.OnClickListener openStore = v -> {
                    Intent i = new Intent(FootprintActivity.this, SellerStoreActivity.class);
                    i.putExtra("seller", row.store.seller);
                    i.putExtra("public_store", true);
                    startActivity(i);
                };
                holder.itemView.setOnClickListener(openStore);
                enter.setOnClickListener(openStore);
                return;
            }
            Product p = row.product;
            ImageView image = holder.itemView.findViewById(1);
            TextView name = holder.itemView.findViewById(2);
            TextView price = holder.itemView.findViewById(3);
            bindProductImage(image, p);
            name.setText(p.name);
            price.setText(String.format(Locale.CHINA, "¥%.2f", p.price));
            holder.itemView.setOnClickListener(v -> {
                Intent i = new Intent(FootprintActivity.this, ProductDetailActivity.class);
                i.putExtra("product_id", p.id);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private void bindStoreAvatar(ImageView avatar, TextView placeholder, StoreFootprint store) {
        String avatarUri = store.avatarUri;
        if (avatarUri != null && !avatarUri.isEmpty()) {
            try {
                if (avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(avatar, avatarUri);
                } else {
                    avatar.setImageURI(Uri.parse(avatarUri));
                }
                avatar.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
                return;
            } catch (Exception ignored) {
            }
        }
        avatar.setVisibility(View.GONE);
        placeholder.setVisibility(View.VISIBLE);
        String storeName = store.storeName;
        placeholder.setText(storeName == null || storeName.isEmpty() ? "店" : storeName.substring(0, 1));
    }

    private void bindProductImage(ImageView image, Product p) {
        switch (p.id) {
            case 1: image.setImageResource(R.mipmap.dami1); break;
            case 2: image.setImageResource(R.mipmap.muer); break;
            case 3: image.setImageResource(R.mipmap.fengmi1); break;
            case 4: image.setImageResource(R.mipmap.shucai1); break;
            case 5: image.setImageResource(R.mipmap.dongchongxiacao1); break;
            case 6: image.setImageResource(R.mipmap.hongshu1); break;
            case 7: image.setImageResource(R.mipmap.shanyao1); break;
            case 8: image.setImageResource(R.mipmap.yangdujun1); break;
            case 9: image.setImageResource(R.mipmap.luronggu1); break;
            case 10: image.setImageResource(R.mipmap.tuedan1); break;
            default:
                if (p.coverUri != null && !p.coverUri.isEmpty()) {
                    try {
                        image.setImageURI(Uri.parse(p.coverUri.split(",")[0]));
                    } catch (Exception e) {
                        image.setImageResource(R.drawable.ic_product_placeholder);
                    }
                } else {
                    image.setImageResource(R.drawable.ic_product_placeholder);
                }
        }
    }
}
