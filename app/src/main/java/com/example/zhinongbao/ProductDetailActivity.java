package com.example.zhinongbao;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Product;
import java.io.IOException;
import java.io.InputStream;

/** 商品详情界面：图片、名称、介绍、价格、加入购物车/购买/购物车入口 */
public class ProductDetailActivity extends AppCompatActivity {

    private int productId;
    private DataManager dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        productId = getIntent().getIntExtra("product_id", -1);
        dm = DataManager.getInstance(this);

        // 查找商品
        Product target = null;
        for (Product p : dm.getProducts()) {
            if (p.id == productId) {
                target = p;
                break;
            }
        }
        if (target == null) {
            finish();
            return;
        }

        final Product product = target;
        String username = dm.getLoggedUser();

        androidx.viewpager2.widget.ViewPager2 vpProductImage = findViewById(R.id.vpProductImage);
        TextView tvImageIndicator = findViewById(R.id.tvImageIndicator);

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
                    String[] uris = product.coverUri.split(",");
                    for (String uri : uris) {
                        images.add(uri);
                    }
                } else {
                    images.add(R.drawable.ic_product_placeholder);
                }
        }

        com.example.zhinongbao.adapter.ProductImageAdapter imageAdapter = new com.example.zhinongbao.adapter.ProductImageAdapter(
                images);
        vpProductImage.setAdapter(imageAdapter);

        if (images.size() <= 1) {
            tvImageIndicator.setVisibility(android.view.View.GONE);
        } else {
            tvImageIndicator.setVisibility(android.view.View.VISIBLE);
            tvImageIndicator.setText("1/" + images.size());
        }

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
        ((TextView) findViewById(R.id.tvDetailProductPrice))
                .setText(String.format("¥%.2f", product.price));

        // 绑定底部图文详情 RecyclerView
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

        // 评价模块
        android.widget.LinearLayout llCommentSection = findViewById(R.id.llCommentSection);
        llCommentSection.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductCommentsActivity.class);
            intent.putExtra("product_id", product.id);
            startActivity(intent);
        });

        // 加入购物车
        ((Button) findViewById(R.id.btnAddCart)).setOnClickListener(v -> {
            dm.addToCart(username, product);
            Toast.makeText(this, "已加入购物车", Toast.LENGTH_SHORT).show();
        });

        // 立即购买 → 生成待支付订单并跳转到订单列表
        ((Button) findViewById(R.id.btnBuy)).setOnClickListener(v -> {
            dm.addOrder(username, product.id, product.name, product.price, 1);
            Toast.makeText(this, "下单成功", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MyOrdersActivity.class));
        });

        // 购物车图标
        findViewById(R.id.ivCartIcon).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        // 联系客服图标（从 assets 加载）
        ImageView ivContactService = findViewById(R.id.ivContactService);
        try {
            InputStream is = getAssets().open("pic/lianxikefu.png");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            ivContactService.setImageBitmap(bmp);
            is.close();
        } catch (IOException ignored) {}
        final String productNameFinal = product.name;
        ivContactService.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("other_user", ChatActivity.SHOP_USERNAME);
            intent.putExtra("product_name", productNameFinal);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dm != null && productId != -1) {
            loadCommentPreview();
        }
    }

    private void loadCommentPreview() {
        int count = dm.getProductCommentCount(productId);
        TextView tvCommentCountTitle = findViewById(R.id.tvCommentCountTitle);
        tvCommentCountTitle.setText("商品评价 (" + count + ")");

        android.widget.LinearLayout llLatestComment = findViewById(R.id.llLatestComment);
        if (count > 0) {
            java.util.List<com.example.zhinongbao.model.ProductComment> comments = dm.getProductComments(productId);
            if (!comments.isEmpty()) {
                com.example.zhinongbao.model.ProductComment latest = comments.get(0);
                llLatestComment.setVisibility(android.view.View.VISIBLE);

                TextView tvUsername = findViewById(R.id.tvCommentUsername);
                TextView tvContent = findViewById(R.id.tvCommentContent);
                ImageView ivAvatar = findViewById(R.id.ivCommentAvatar);

                tvUsername.setText(
                        latest.nickname != null && !latest.nickname.isEmpty() ? latest.nickname : latest.username);
                tvContent.setText(latest.content);

                if (latest.avatarUri != null && !latest.avatarUri.isEmpty()) {
                    try {
                        ivAvatar.setImageURI(android.net.Uri.parse(latest.avatarUri));
                    } catch (Exception e) {
                        ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
                    }
                } else {
                    ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
                }
            }
        } else {
            llLatestComment.setVisibility(android.view.View.GONE);
        }
    }
}
