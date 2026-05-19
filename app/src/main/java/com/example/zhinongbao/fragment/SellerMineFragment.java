package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.AddProductActivity;
import com.example.zhinongbao.ArticleDetailActivity;
import com.example.zhinongbao.MainActivity;
import com.example.zhinongbao.ProfileEditActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.SellerMyProductsActivity;
import com.example.zhinongbao.SellerOrdersActivity;
import com.example.zhinongbao.SellerPurchaseMgmtActivity;
import com.example.zhinongbao.SellerStoreActivity;
import com.example.zhinongbao.SettingsActivity;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.User;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SellerMineFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindViews(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null)
            bindViews(getView());
    }

    private void bindViews(View view) {
        DataManager dm = DataManager.getInstance(requireContext());
        String username = dm.getLoggedUser();
        String nickname = dm.getNickname(username);

        // 店铺名称（用昵称代替）
        ((TextView) view.findViewById(R.id.tvSellerShopName))
                .setText(nickname + "的店铺 ›");

        // 昵称
        ((TextView) view.findViewById(R.id.tvSellerNickname)).setText(nickname);

        // 头像
        ImageView ivAvatar = view.findViewById(R.id.ivSellerAvatar);
        TextView tvInitial = view.findViewById(R.id.tvSellerAvatarInitial);
        String avatarUri = dm.getAvatarUri(username);
        if (avatarUri != null) {
            try {
                if (avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(ivAvatar, avatarUri);
                } else {
                    ivAvatar.setImageURI(Uri.parse(avatarUri));
                }
                ivAvatar.setVisibility(View.VISIBLE);
                tvInitial.setVisibility(View.GONE);
            } catch (Exception e) {
                ivAvatar.setVisibility(View.GONE);
                tvInitial.setVisibility(View.VISIBLE);
            }
        } else {
            tvInitial.setText((!username.isEmpty())
                    ? String.valueOf(username.charAt(0)).toUpperCase()
                    : "我");
            ivAvatar.setVisibility(View.GONE);
            tvInitial.setVisibility(View.VISIBLE);
        }

        // 加载快捷功能图标（来自 assets/pic）
        loadAssetImage(view.findViewById(R.id.ivMyProducts), "wodehuopin.png");
        loadAssetImage(view.findViewById(R.id.ivPurchaseMgmt), "caigouguanli.png");
        loadAssetImage(view.findViewById(R.id.ivOrderMgmt), "dingdanguanli.png");
        loadAssetImage(view.findViewById(R.id.ivShopMgmt), "dianpuguanli.png");

        // 加载订单状态图标
        loadAssetImage(view.findViewById(R.id.ivSellerOrderPending), "daifukuan.png");
        loadAssetImage(view.findViewById(R.id.ivSellerOrderShipping), "daifahuo.png");
        loadAssetImage(view.findViewById(R.id.ivSellerOrderSent), "daishouhuo.png");
        loadAssetImage(view.findViewById(R.id.ivSellerOrderAfterSale), "tuikuanshouhou.png");

        // ── 点击事件 ──

        // 去买货：切换为买家身份
        view.findViewById(R.id.btnGoShopping).setOnClickListener(v -> {
            int role = dm.getUserRole(username);
            if (role == User.ROLE_BOTH) {
                dm.setActiveRole(User.ROLE_BUYER);
                // 重启 MainActivity 以刷新身份
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "您目前没有买家身份", Toast.LENGTH_SHORT).show();
            }
        });

        // 编辑资料
        view.findViewById(R.id.layoutSellerProfile).setOnClickListener(
                v -> startActivity(new Intent(getContext(), ProfileEditActivity.class)));

        // 快捷功能
        view.findViewById(R.id.quickMyProducts).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerMyProductsActivity.class)));
        view.findViewById(R.id.quickPurchaseMgmt).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerPurchaseMgmtActivity.class)));
        view.findViewById(R.id.quickOrderMgmt).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerOrdersActivity.class)));
        view.findViewById(R.id.quickShopMgmt).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerStoreActivity.class)));

        // 订单按钮
        view.findViewById(R.id.tvSellerAllOrders).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerOrdersActivity.class)));
        view.findViewById(R.id.sellerOrderPending).setOnClickListener(v -> {
            Intent i = new Intent(getContext(), SellerOrdersActivity.class);
            i.putExtra("filter", "pending");
            startActivity(i);
        });
        view.findViewById(R.id.sellerOrderShipping).setOnClickListener(v -> {
            Intent i = new Intent(getContext(), SellerOrdersActivity.class);
            i.putExtra("filter", "paid");
            startActivity(i);
        });
        view.findViewById(R.id.sellerOrderSent).setOnClickListener(v -> {
            Intent i = new Intent(getContext(), SellerOrdersActivity.class);
            i.putExtra("filter", "shipped");
            startActivity(i);
        });
        view.findViewById(R.id.sellerOrderAfterSale).setOnClickListener(v -> {
            Intent i = new Intent(getContext(), SellerOrdersActivity.class);
            i.putExtra("filter", "refund");
            startActivity(i);
        });

        // 初始化新闻资讯
        RecyclerView rvNews = view.findViewById(R.id.rvNews);
        if (rvNews != null) {
            rvNews.setLayoutManager(new LinearLayoutManager(getContext()));
            List<Article> articles = dm.getArticles();
            ArticleAdapter articleAdapter = new ArticleAdapter(articles, article -> {
                Intent intent = new Intent(getContext(), ArticleDetailActivity.class);
                intent.putExtra("article_id", article.id);
                startActivity(intent);
            }, dm, username);
            articleAdapter.setHideCategory(true);
            rvNews.setAdapter(articleAdapter);
        }
    }

    private void loadAssetImage(ImageView iv, String filename) {
        if (iv == null)
            return;
        try {
            InputStream is = requireContext().getAssets().open("pic/" + filename);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            iv.setImageBitmap(bmp);
            is.close();
        } catch (IOException ignored) {
        }
    }
}
