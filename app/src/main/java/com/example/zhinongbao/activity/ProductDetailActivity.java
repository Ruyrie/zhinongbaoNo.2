package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
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

import androidx.core.content.FileProvider;

import android.content.ClipData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
                new com.example.zhinongbao.adapter.ProductImageAdapter(images, ImageView.ScaleType.FIT_XY);
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
        findViewById(R.id.btnShareProduct).setOnClickListener(v -> shareProduct(product));
    }

    private void shareProduct(Product product) {
        String shareText = buildShareText(product);
        Uri imageUri = createShareImageUri(product);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_TEXT, shareText);
        share.putExtra(Intent.EXTRA_TITLE, product.name);
        if (imageUri != null) {
            share.setType("image/jpeg");
            share.putExtra(Intent.EXTRA_STREAM, imageUri);
            share.setClipData(ClipData.newUri(getContentResolver(), "商品分享图", imageUri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            share.setType("text/plain");
        }
        startActivity(Intent.createChooser(share, "分享商品"));
    }

    private String buildShareText(Product product) {
        StringBuilder builder = new StringBuilder();
        builder.append(product.name)
                .append("\n价格：")
                .append(String.format("¥%.2f", product.price));
        if (product.desc != null && !product.desc.trim().isEmpty()) {
            builder.append("\n").append(product.desc.trim());
        }
        builder.append("\n来自支农宝");
        return builder.toString();
    }

    private Uri createShareImageUri(Product product) {
        Bitmap bitmap = createSharePosterBitmap(product);
        if (bitmap == null) {
            return null;
        }
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists() && !imagePath.mkdirs()) {
            return null;
        }
        File imageFile = new File(imagePath, "share_product_" + product.id + ".jpg");
        try (OutputStream os = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os);
            os.flush();
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Bitmap createSharePosterBitmap(Product product) {
        final int width = 1080;
        final int height = 1500;
        final int imageHeight = 760;
        Bitmap poster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(poster);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        Bitmap productBitmap = loadShareBitmap(product);
        if (productBitmap != null) {
            canvas.drawBitmap(productBitmap, null, new Rect(0, 0, width, imageHeight), paint);
        } else {
            paint.setColor(0xFFF2F3F5);
            canvas.drawRect(0, 0, width, imageHeight, paint);
            paint.setColor(0xFF9AA0A6);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(44);
            canvas.drawText("支农宝优选商品", width / 2f, imageHeight / 2f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        int left = 70;
        int right = width - 70;
        float y = imageHeight + 86;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(0xFFE53935);
        paint.setTextSize(58);
        canvas.drawText(String.format("¥%.2f", product.price), left, y, paint);

        y += 88;
        paint.setColor(0xFF202124);
        paint.setTextSize(48);
        y = drawWrappedText(canvas, product.name, paint, left, y, right - left, 2, 64);

        String desc = product.desc == null ? "" : product.desc.trim();
        if (!desc.isEmpty()) {
            y += 30;
            paint.setTypeface(Typeface.DEFAULT);
            paint.setColor(0xFF5F6368);
            paint.setTextSize(34);
            y = drawWrappedText(canvas, desc, paint, left, y, right - left, 3, 48);
        }

        y += 54;
        paint.setColor(0xFFF4F7F4);
        canvas.drawRoundRect(left, y, right, y + 104, dp(14), dp(14), paint);
        paint.setColor(0xFF2E7D32);
        paint.setTextSize(32);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String params = "品牌 " + displayParam(product.brand) + "  产地 " + displayParam(product.origin)
                + "  规格 " + displayParam(product.spec);
        drawWrappedText(canvas, params, paint, left + 28, y + 42, right - left - 56, 1, 42);

        paint.setColor(0xFFE8EAED);
        canvas.drawLine(left, height - 160, right, height - 160, paint);
        paint.setColor(0xFF202124);
        paint.setTextSize(38);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("支农宝", left, height - 92, paint);
        paint.setColor(0xFF6B7280);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("发现优质农产品，分享给身边的人", left, height - 46, paint);

        return poster;
    }

    private float drawWrappedText(Canvas canvas, String text, Paint paint, float x, float y,
                                  float maxWidth, int maxLines, float lineHeight) {
        if (text == null || text.trim().isEmpty()) {
            return y;
        }
        String rest = text.trim();
        int line = 0;
        while (!rest.isEmpty() && line < maxLines) {
            int count = paint.breakText(rest, true, maxWidth, null);
            String current = rest.substring(0, count).trim();
            rest = rest.substring(count).trim();
            if (line == maxLines - 1 && !rest.isEmpty()) {
                current = trimToWidth(current + "...", paint, maxWidth);
            }
            canvas.drawText(current, x, y, paint);
            y += lineHeight;
            line++;
        }
        return y;
    }

    private String trimToWidth(String text, Paint paint, float maxWidth) {
        String value = text;
        while (value.length() > 3 && paint.measureText(value) > maxWidth) {
            value = value.substring(0, value.length() - 4) + "...";
        }
        return value;
    }

    private Bitmap loadShareBitmap(Product product) {
        java.util.List<Object> images = buildImages(product);
        if (images.isEmpty()) {
            return null;
        }
        Object first = images.get(0);
        if (first instanceof Integer) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), (Integer) first, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(bounds, 1080, 760);
            return BitmapFactory.decodeResource(getResources(), (Integer) first, options);
        }
        if (first instanceof String) {
            Uri uri = Uri.parse((String) first);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, bounds);
            } catch (Exception ignored) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(bounds, 1080, 760);
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is, null, options);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        while (height / inSampleSize > reqHeight * 2 || width / inSampleSize > reqWidth * 2) {
            inSampleSize *= 2;
        }
        return inSampleSize;
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
            tvFavorite.setTextColor(favorited ? 0xFFE53935 : 0xFF999999);
        }
        if (ivFavorite != null) {
            ivFavorite.setImageResource(favorited
                    ? R.drawable.ic_star_filled_red
                    : R.drawable.ic_star_outline_gray);
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
