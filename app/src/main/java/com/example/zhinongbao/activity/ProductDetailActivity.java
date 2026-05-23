package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.ProductComment;
import com.example.zhinongbao.mvp.productdetail.ProductDetailContract;
import com.example.zhinongbao.mvp.productdetail.ProductDetailPresenter;
import com.example.zhinongbao.utils.DialogUtils;

import java.io.IOException;
import java.io.InputStream;

/** 商品详情界面：图片、名称、介绍、价格、加入购物车/购买/购物车入口 */
public class ProductDetailActivity extends BaseMvpActivity<ProductDetailContract.Presenter>
        implements ProductDetailContract.View {

    private int productId;
    private Product product;
    private TextView tvFavorite;
    private ImageView ivFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        productId = getIntent().getIntExtra("product_id", -1);
        new ProductDetailPresenter(this, this, productId).start();
    }

    @Override
    public void showProduct(Product product, String username, String seller, boolean ownProduct, boolean favorited) {
        this.product = product;
        androidx.viewpager2.widget.ViewPager2 vpProductImage = findViewById(R.id.vpProductImage);
        TextView tvImageIndicator = findViewById(R.id.tvImageIndicator);
        java.util.List<Object> images = buildImages(product);

        com.example.zhinongbao.adapter.ProductImageAdapter imageAdapter =
                new com.example.zhinongbao.adapter.ProductImageAdapter(images);
        vpProductImage.setAdapter(imageAdapter);

        tvImageIndicator.setVisibility(images.size() <= 1 ? android.view.View.GONE : android.view.View.VISIBLE);
        tvImageIndicator.setText("1/" + images.size());
        vpProductImage.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (images.size() > 1) {
                    tvImageIndicator.setText((position + 1) + "/" + images.size());
                }
            }
        });

        ((TextView) findViewById(R.id.tvDetailProductName)).setText(product.name);
        ((TextView) findViewById(R.id.tvDetailProductDesc)).setText(product.desc);
        ((TextView) findViewById(R.id.tvDetailProductPrice)).setText(String.format("¥%.2f", product.price));
        ((TextView) findViewById(R.id.tvDetailViewCount)).setText(product.viewCount + " 浏览");
        loadAssetImage(findViewById(R.id.ivDetailStore), "dianpu.png");
        loadAssetImage(findViewById(R.id.ivDetailService), "lianxikefu.png");
        loadAssetImage(findViewById(R.id.ivDetailPhone), "dianhua.png");
        tvFavorite = findViewById(R.id.tvProductFavorite);
        ivFavorite = findViewById(R.id.ivProductFavorite);
        setFavoriteState(favorited);

        bindDetailImages(images);
        bindProductParams(product);
        bindCommentSection(product);
        bindStoreActions(product, seller);
        bindFavoriteAndShare(product);
        bindBottomActions(product, ownProduct);
    }

    private void bindProductParams(Product product) {
        TextView summary = findViewById(R.id.tvProductParamsSummary);
        String brand = displayParam(product.brand);
        String origin = displayParam(product.origin);
        String spec = displayParam(product.spec);
        String packageType = displayParam(product.packageType);
        summary.setText("品牌 " + brand + "  产地 " + origin + "  规格 " + spec);

        findViewById(R.id.llProductParams).setOnClickListener(v -> {
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.addView(createParamRow("品牌", brand));
            content.addView(createParamRow("产地", origin));
            content.addView(createParamRow("规格", spec));
            content.addView(createParamRow("包装方式", packageType));
            showParamsDialog(content);
        });
    }

    private void showParamsDialog(LinearLayout content) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackgroundResource(R.drawable.bg_dialog_card);

        TextView title = new TextView(this);
        title.setText("商品参数");
        title.setTextColor(0xFF212529);
        title.setTextSize(21);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        root.addView(title);
        root.addView(content);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(root).create();
        TextView close = new TextView(this);
        close.setText("关闭");
        close.setTextColor(Color.WHITE);
        close.setTextSize(15);
        close.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundResource(R.drawable.bg_auth_green_button);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        closeLp.topMargin = dp(22);
        root.addView(close, closeLp);
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = getResources().getDisplayMetrics().widthPixels - dp(48);
                window.setLayout(Math.min(width, dp(360)), LinearLayout.LayoutParams.WRAP_CONTENT);
            }
        });
        dialog.show();
    }

    private TextView createParamRow(String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label + "：" + value);
        tv.setTextColor(0xFF333333);
        tv.setTextSize(15);
        tv.setPadding(0, dp(7), 0, dp(7));
        return tv;
    }

    private String displayParam(String value) {
        return value == null || value.trim().isEmpty() ? "暂无填写" : value.trim();
    }

    private void bindDetailImages(java.util.List<Object> images) {
        androidx.recyclerview.widget.RecyclerView rvDetailImages = findViewById(R.id.rvDetailImages);
        rvDetailImages.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvDetailImages.setAdapter(
                new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                    @androidx.annotation.NonNull
                    @Override
                    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(
                            @androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
                        ImageView iv = new ImageView(parent.getContext());
                        iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                        iv.setAdjustViewBounds(true);
                        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        return new androidx.recyclerview.widget.RecyclerView.ViewHolder(iv) {
                        };
                    }

                    @Override
                    public void onBindViewHolder(
                            @androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder,
                            int position) {
                        ImageView iv = (ImageView) holder.itemView;
                        Object item = images.get(position);
                        if (item instanceof Integer) {
                            iv.setImageResource((Integer) item);
                        } else if (item instanceof String) {
                            try {
                                iv.setImageURI(android.net.Uri.parse((String) item));
                            } catch (Exception e) {
                                iv.setImageResource(R.drawable.ic_product_placeholder);
                            }
                        }
                    }

                    @Override
                    public int getItemCount() {
                        return images.size();
                    }
                });
    }

    private void bindCommentSection(Product product) {
        android.widget.LinearLayout llCommentSection = findViewById(R.id.llCommentSection);
        llCommentSection.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductCommentsActivity.class);
            intent.putExtra("product_id", product.id);
            startActivity(intent);
        });
    }

    private void bindStoreActions(Product product, String seller) {
        findViewById(R.id.btnOpenStore).setOnClickListener(v -> {
            Intent intent = new Intent(this, SellerStoreActivity.class);
            intent.putExtra("seller", seller);
            startActivity(intent);
        });

        findViewById(R.id.btnContactService).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("other_user", seller);
            intent.putExtra("product_name", product.name);
            startActivity(intent);
        });

        findViewById(R.id.btnCallShop).setOnClickListener(v -> {
            String phone = presenter.getStorePhone();
            if (phone == null || phone.trim().isEmpty()) {
                showToast("商铺暂未填写电话");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        });
    }

    private void bindFavoriteAndShare(Product product) {
        findViewById(R.id.btnFavoriteProduct).setOnClickListener(v -> presenter.toggleFavorite());
        findViewById(R.id.btnShareProduct).setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,
                    product.name + "\n价格：" + String.format("¥%.2f", product.price) + "\n来自支农宝");
            startActivity(Intent.createChooser(share, "分享商品"));
        });
    }

    private void bindBottomActions(Product product, boolean ownProduct) {
        Button btnAddCart = findViewById(R.id.btnAddCart);
        Button btnBuy = findViewById(R.id.btnBuy);

        if (ownProduct) {
            findViewById(R.id.btnOpenStore).setVisibility(android.view.View.GONE);
            findViewById(R.id.btnContactService).setVisibility(android.view.View.GONE);
            findViewById(R.id.btnCallShop).setVisibility(android.view.View.GONE);
            findViewById(R.id.btnFavoriteProduct).setVisibility(android.view.View.GONE);
            findViewById(R.id.btnShareProduct).setVisibility(android.view.View.GONE);

            btnAddCart.setText("编辑商品");
            btnAddCart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2F80ED));
            btnAddCart.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddProductActivity.class);
                intent.putExtra("product_id", product.id);
                startActivity(intent);
            });

            btnBuy.setText("下架商品");
            btnBuy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE53935));
            btnBuy.setOnClickListener(v -> DialogUtils.showConfirm(this, "下架商品",
                    "确认将「" + product.name + "」下架？下架后买家将无法购买。",
                    "取消", "确认下架", true, () -> {
                        presenter.deleteProduct();
                        return true;
                    }));
        } else {
            btnAddCart.setOnClickListener(v -> presenter.addToCart());
            btnBuy.setOnClickListener(v -> presenter.buyNow());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null && productId != -1) {
            presenter.refreshComments();
        }
    }

    @Override
    public void showCommentPreview(int count, ProductComment latest) {
        TextView tvCommentCountTitle = findViewById(R.id.tvCommentCountTitle);
        tvCommentCountTitle.setText("商品评价 (" + count + ")");

        android.widget.LinearLayout llLatestComment = findViewById(R.id.llLatestComment);
        if (latest == null) {
            llLatestComment.setVisibility(android.view.View.GONE);
            return;
        }

        llLatestComment.setVisibility(android.view.View.VISIBLE);
        TextView tvUsername = findViewById(R.id.tvCommentUsername);
        TextView tvContent = findViewById(R.id.tvCommentContent);
        ImageView ivAvatar = findViewById(R.id.ivCommentAvatar);

        tvUsername.setText(latest.nickname != null && !latest.nickname.isEmpty() ? latest.nickname : latest.username);
        tvContent.setText(latest.content);

        if (latest.avatarUri != null && !latest.avatarUri.isEmpty()) {
            try {
                if (latest.avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(ivAvatar, latest.avatarUri);
                } else {
                    ivAvatar.setImageURI(android.net.Uri.parse(latest.avatarUri));
                }
            } catch (Exception e) {
                ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setFavoriteState(boolean favorited) {
        if (tvFavorite != null) {
            tvFavorite.setText(favorited ? "已收藏" : "收藏");
        }
        if (ivFavorite != null) {
            loadAssetImage(ivFavorite, favorited ? "yishoucang.png" : "shoucang.png");
        }
    }

    @Override
    public void openAddressManager() {
        startActivity(new Intent(this, AddressManagerActivity.class));
    }

    @Override
    public void openMyOrders() {
        startActivity(new Intent(this, MyOrdersActivity.class));
    }

    @Override
    public void closePage() {
        finish();
    }

    private java.util.List<Object> buildImages(Product product) {
        java.util.List<Object> images = new java.util.ArrayList<>();
        switch (product.id) {
            case 1:
                images.add(R.mipmap.dami1);
                images.add(R.mipmap.dami2);
                images.add(R.mipmap.dami3);
                break;
            case 2:
                images.add(R.mipmap.muer);
                images.add(R.mipmap.muer2);
                images.add(R.mipmap.muer3);
                images.add(R.mipmap.muer4);
                images.add(R.mipmap.muer5);
                break;
            case 3:
                images.add(R.mipmap.fengmi1);
                images.add(R.mipmap.fengmi2);
                images.add(R.mipmap.fengmi3);
                break;
            case 4:
                images.add(R.mipmap.shucai1);
                images.add(R.mipmap.shucai2);
                images.add(R.mipmap.shucai3);
                images.add(R.mipmap.shucai4);
                break;
            case 5:
                images.add(R.mipmap.dongchongxiacao1);
                images.add(R.mipmap.dongchongxiacao2);
                images.add(R.mipmap.dongchongxiacao3);
                break;
            case 6:
                images.add(R.mipmap.hongshu1);
                images.add(R.mipmap.hongshu2);
                images.add(R.mipmap.hongshu3);
                images.add(R.mipmap.hongshu4);
                break;
            case 7:
                images.add(R.mipmap.shanyao1);
                images.add(R.mipmap.shanyao2);
                images.add(R.mipmap.shanyao3);
                break;
            case 8:
                images.add(R.mipmap.yangdujun1);
                images.add(R.mipmap.yangdujun2);
                break;
            case 9:
                images.add(R.mipmap.luronggu1);
                images.add(R.mipmap.luronggu2);
                images.add(R.mipmap.luronggu3);
                images.add(R.mipmap.luronggu4);
                break;
            case 10:
                images.add(R.mipmap.tuedan1);
                images.add(R.mipmap.tuedan2);
                images.add(R.mipmap.tuedan3);
                images.add(R.mipmap.tuedan4);
                break;
            default:
                if (product.coverUri != null && !product.coverUri.isEmpty()) {
                    for (String uri : product.coverUri.split(",")) {
                        images.add(uri);
                    }
                } else {
                    images.add(R.drawable.ic_product_placeholder);
                }
        }
        return images;
    }

    private void loadAssetImage(ImageView iv, String filename) {
        if (iv == null)
            return;
        try (InputStream is = getAssets().open("pic/" + filename)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            iv.setImageBitmap(bmp);
        } catch (IOException ignored) {
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
