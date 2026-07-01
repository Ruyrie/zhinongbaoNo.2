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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.mvp.sellerstore.SellerStoreContract;
import com.example.zhinongbao.mvp.sellerstore.SellerStorePresenter;
import com.example.zhinongbao.repository.UserRepository;
import com.example.zhinongbao.utils.DialogUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【卖家店铺 / Seller Store】View（Activity 页面）
 * 整体逻辑（关键步骤）：
 *   1. onCreate 加载布局 activity_seller_store.xml，读取 intent 的 seller/public_store 参数。
 *   2. 创建 SellerStorePresenter，配置 RecyclerView 与内部 StoreProductAdapter。
 *   3. Presenter 回调 showStoreMeta/showProducts/showStats/showFollowState 时填充界面。
 *   4. 点击商品进 ProductDetailActivity；添加商品进 AddProductActivity；改资料弹对话框提交给 Presenter。
 *   5. onResume 回到本页重新 loadStore 刷新。
 * 数据来源：不直接碰数据库；数据由 SellerStorePresenter 经多个 Repository
 *   （内部走 ContentProvider 访问 SQLite）取得后回调本类。
 * 配合的文件：接口 = mvp/sellerstore/SellerStoreContract；Presenter = SellerStorePresenter；
 *   店铺商品适配器 = 本文件内部类 StoreProductAdapter；行布局 = res/layout/item_seller_product.xml；
 *   页面布局 = res/layout/activity_seller_store.xml；跳转页 = ProductDetailActivity/AddProductActivity。
 * 在 MVP 数据流中的位置：View（界面层）。
 * 提示：在 IDE 里搜索「卖家店铺」可看本组相关文件。
 * ============================================================
 */
public class SellerStoreActivity extends BaseMvpActivity<SellerStoreContract.Presenter> implements SellerStoreContract.View {

    private String seller;
    private boolean isOwnStore;
    private boolean publicStoreMode;
    private UserRepository userRepository;
    private final List<Product> products = new ArrayList<>();
    private StoreProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_store);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        new SellerStorePresenter(this, this);
        userRepository = new UserRepository(getApplicationContext());
        seller = getIntent().getStringExtra("seller");
        publicStoreMode = getIntent().getBooleanExtra("public_store", false);
        if (seller == null || seller.isEmpty()) seller = presenter.getCurrentUser();

        // 返回
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        TextView tvAddProduct = findViewById(R.id.tvAddProduct);
        tvAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        TextView tvEditStoreInfo = findViewById(R.id.tvEditStoreInfo);
        tvEditStoreInfo.setOnClickListener(v -> showEditStoreDialog());

        findViewById(R.id.tvFollowStore).setOnClickListener(v -> presenter.toggleFollow());

        // 列表
        RecyclerView rv = findViewById(R.id.rvProducts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StoreProductAdapter(products, isOwnStore, product -> {
            // 点击商品→详情
            Intent i = new Intent(this, ProductDetailActivity.class);
            i.putExtra("product_id", product.id);
            startActivity(i);
        }, product -> {
            // 下架（软状态，可重新上架）
            DialogUtils.showConfirm(this, "下架商品",
                    "确认将「" + product.name + "」下架？下架后买家将无法购买，可随时重新上架。",
                    "取消", "确认下架", true, () -> {
                        presenter.delistProduct(product.id);
                        Toast.makeText(this, "已下架", Toast.LENGTH_SHORT).show();
                        return true;
                    });
        }, product -> {
            // 重新上架
            presenter.relistProduct(product.id);
            Toast.makeText(this, "已重新上架", Toast.LENGTH_SHORT).show();
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
    public void showStoreMeta(String seller, String storeName, String storePhone, String avatarUri, boolean ownStore) {
        this.seller = seller;
        boolean editableOwnStore = ownStore && !publicStoreMode;
        this.isOwnStore = editableOwnStore;
        findViewById(R.id.tvAddProduct).setVisibility(editableOwnStore ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvEditStoreInfo).setVisibility(editableOwnStore ? View.VISIBLE : View.GONE);
        findViewById(R.id.llSalesStats).setVisibility(editableOwnStore ? View.VISIBLE : View.GONE);
        if (adapter != null) {
            adapter.setOwnStore(editableOwnStore);
        }
        ((TextView) findViewById(R.id.tvStoreName)).setText(editableOwnStore ? "我的店铺" : storeName);
        ((TextView) findViewById(R.id.tvStoreDisplayName)).setText(storeName);
        ((TextView) findViewById(R.id.tvStoreSeller)).setText("卖家 " + seller);
        String phone = storePhone;
        ((TextView) findViewById(R.id.tvStorePhone)).setText(
                phone == null || phone.isEmpty() ? "电话：未填写" : "电话：" + phone);
        String latestAvatarUri = userRepository == null ? avatarUri : userRepository.getAvatarUri(seller);
        if (latestAvatarUri == null || latestAvatarUri.isEmpty()) {
            latestAvatarUri = avatarUri;
        }
        bindStoreAvatar((ImageView) findViewById(R.id.ivStoreAvatar),
                (TextView) findViewById(R.id.tvStoreAvatarPlaceholder), latestAvatarUri, storeName);
    }

    @Override
    public void showFollowState(boolean following, boolean canFollow) {
        TextView btnFollow = findViewById(R.id.tvFollowStore);
        btnFollow.setVisibility(canFollow ? View.VISIBLE : View.GONE);
        btnFollow.setText(following ? "已关注" : "+ 关注");
        btnFollow.setAlpha(following ? 0.7f : 1f);
    }

    private void bindStoreAvatar(ImageView avatar, TextView placeholder, String avatarUri, String storeName) {
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
                // Fall through to default avatar.
            }
        }
        avatar.setVisibility(View.GONE);
        placeholder.setVisibility(View.VISIBLE);
        placeholder.setText(storeName == null || storeName.isEmpty() ? "店" : storeName.substring(0, 1));
        avatar.setContentDescription((storeName == null ? "店铺" : storeName) + "头像");
    }

    // 弹出「修改店铺信息」对话框，收集店铺名称与电话后交给 Presenter 保存
    private void showEditStoreDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView nameLabel = createDialogLabel("店铺名称");
        box.addView(nameLabel);
        EditText etName = DialogUtils.createInput(this, "请输入店铺名称", false);
        etName.setHint("店铺名称");
        etName.setText(((TextView) findViewById(R.id.tvStoreDisplayName)).getText());
        box.addView(etName);

        TextView phoneLabel = createDialogLabel("联系电话");
        phoneLabel.setPadding(0, dp(14), 0, 0);
        box.addView(phoneLabel);
        EditText etPhone = DialogUtils.createInput(this, "请输入联系电话", false);
        etPhone.setHint("商铺电话");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        String phoneText = ((TextView) findViewById(R.id.tvStorePhone)).getText().toString().replace("电话：", "");
        etPhone.setText("未填写".equals(phoneText) ? "" : phoneText);
        box.addView(etPhone);

        DialogUtils.showContent(this, "修改店铺信息", "完善店铺名称和联系电话，方便买家确认商品来源。",
                box, "取消", "保存", false, () -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this, "店铺名称和电话不能为空", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    presenter.updateStoreInfo(name, phone);
                    return true;
                });
    }

    private TextView createDialogLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(0xFF212529);
        label.setTextSize(14);
        label.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return label;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void showProducts(List<Product> fresh) {
        products.clear();
        products.addAll(fresh);
        if (adapter != null) adapter.notifyDataSetChanged();

        RecyclerView rv = findViewById(R.id.rvProducts);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        int onSaleCount = 0;
        for (Product p : products) {
            if (!p.isOffShelf()) onSaleCount++;
        }
        ((TextView) findViewById(R.id.tvStoreProductCount)).setText("商品 " + products.size() + " 件");
        ((TextView) findViewById(R.id.tvProductSummary)).setText(onSaleCount + " 件在售");
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

    // 店铺商品列表适配器（内部类）

    interface OnProductClick { void onClick(Product p); } // 商品行点击/上架/下架统一回调

    // 把店铺商品渲染成一行（封面、名称、价格、销量/下架标记），本人店铺额外显示上/下架按钮
    static class StoreProductAdapter extends RecyclerView.Adapter<StoreProductAdapter.VH> {
        private final List<Product> items;
        private boolean isOwn;
        private final OnProductClick clickListener;
        private final OnProductClick delistListener;
        private final OnProductClick relistListener;
        private final ProductSalesResolver salesResolver;

        interface ProductSalesResolver {
            int getProductOrderCount(int productId);
        }

        StoreProductAdapter(List<Product> items, boolean isOwn,
                OnProductClick click, OnProductClick delist, OnProductClick relist,
                ProductSalesResolver salesResolver) {
            this.items = items;
            this.isOwn = isOwn;
            this.clickListener = click;
            this.delistListener = delist;
            this.relistListener = relist;
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
            h.tvSales.setText(p.isOffShelf() ? "已下架" : "已售 " + orderCount + " 件");
            // 已下架商品整行降低不透明度，给出视觉区分
            h.itemView.setAlpha(p.isOffShelf() ? 0.55f : 1f);

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

            // 上下架按钮：在售→「下架」，已下架→「上架」
            if (isOwn) {
                h.btnDelist.setVisibility(View.VISIBLE);
                if (p.isOffShelf()) {
                    h.btnDelist.setText("上架");
                    h.btnDelist.setOnClickListener(v -> relistListener.onClick(p));
                } else {
                    h.btnDelist.setText("下架");
                    h.btnDelist.setOnClickListener(v -> delistListener.onClick(p));
                }
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
