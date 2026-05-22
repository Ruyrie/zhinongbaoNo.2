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
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zhinongbao.activity.AddProductActivity;
import com.example.zhinongbao.activity.ArticleDetailActivity;
import com.example.zhinongbao.activity.MainActivity;
import com.example.zhinongbao.activity.ProfileEditActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.activity.SellerMyProductsActivity;
import com.example.zhinongbao.activity.SellerOrdersActivity;
import com.example.zhinongbao.activity.SellerPurchaseMgmtActivity;
import com.example.zhinongbao.activity.SellerSalesAnalysisActivity;
import com.example.zhinongbao.activity.SellerStoreActivity;
import com.example.zhinongbao.activity.SettingsActivity;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.mvp.sellermine.SellerMineContract;
import com.example.zhinongbao.mvp.sellermine.SellerMinePresenter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SellerMineFragment extends BaseMvpFragment<SellerMineContract.Presenter> implements SellerMineContract.View {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_mine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindStaticViews(view);
        new SellerMinePresenter(requireContext(), this).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null)
            presenter.start();
    }

    private void bindStaticViews(View view) {
        loadAssetImage(view.findViewById(R.id.ivMyProducts), "wodehuopin.png");
        loadAssetImage(view.findViewById(R.id.ivPurchaseMgmt), "caigouguanli.png");
        loadAssetImage(view.findViewById(R.id.ivOrderMgmt), "dingdanguanli.png");
        loadAssetImage(view.findViewById(R.id.ivShopMgmt), "dianpuguanli.png");
        loadAssetImage(view.findViewById(R.id.ivSellerOrderPending), "daifukuan.png");
        loadAssetImage(view.findViewById(R.id.ivSellerOrderShipping), "daifahuo.png");
        loadAssetImage(view.findViewById(R.id.ivSellerOrderSent), "daishouhuo.png");
        loadAssetImage(view.findViewById(R.id.ivSellerOrderAfterSale), "tuikuanshouhou.png");

        view.findViewById(R.id.btnGoShopping).setOnClickListener(v -> presenter.switchToBuyer());
        view.findViewById(R.id.layoutSellerProfile).setOnClickListener(
                v -> startActivity(new Intent(getContext(), ProfileEditActivity.class)));
        view.findViewById(R.id.quickMyProducts).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerMyProductsActivity.class)));
        view.findViewById(R.id.quickPurchaseMgmt).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerPurchaseMgmtActivity.class)));
        view.findViewById(R.id.quickOrderMgmt).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerOrdersActivity.class)));
        view.findViewById(R.id.quickShopMgmt).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerStoreActivity.class)));
        view.findViewById(R.id.layoutTodaySales).setOnClickListener(v -> openSalesOrders("today"));
        view.findViewById(R.id.layoutMonthSales).setOnClickListener(v -> openSalesOrders("month"));
        view.findViewById(R.id.layoutTotalSales).setOnClickListener(v -> openSalesOrders("all"));
        view.findViewById(R.id.tvSellerAllOrders).setOnClickListener(
                v -> startActivity(new Intent(getContext(), SellerOrdersActivity.class)));
        view.findViewById(R.id.sellerOrderPending).setOnClickListener(v -> openSellerOrders("pending"));
        view.findViewById(R.id.sellerOrderShipping).setOnClickListener(v -> openSellerOrders("paid"));
        view.findViewById(R.id.sellerOrderSent).setOnClickListener(v -> openSellerOrders("shipped"));
        view.findViewById(R.id.sellerOrderAfterSale).setOnClickListener(v -> openSellerOrders("refund"));
    }

    @Override
    public void renderSeller(String username, String storeName, String nickname, String avatarUri,
            double todayRevenue, double monthRevenue, double totalRevenue) {
        View view = getView();
        if (view == null) return;

        // 店铺名称（用昵称代替）
        ((TextView) view.findViewById(R.id.tvSellerShopName))
                .setText(storeName + " ›");

        // 昵称
        ((TextView) view.findViewById(R.id.tvSellerNickname)).setText(nickname);

        // 头像
        ImageView ivAvatar = view.findViewById(R.id.ivSellerAvatar);
        TextView tvInitial = view.findViewById(R.id.tvSellerAvatarInitial);
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

        ((TextView) view.findViewById(R.id.tvTodaySales))
                .setText(String.format(java.util.Locale.getDefault(), "%.2f", todayRevenue));
        ((TextView) view.findViewById(R.id.tvMonthSales))
                .setText(String.format(java.util.Locale.getDefault(), "%.2f", monthRevenue));
        ((TextView) view.findViewById(R.id.tvTotalSales))
                .setText(String.format(java.util.Locale.getDefault(), "%.2f", totalRevenue));
    }

    @Override
    public void renderNews(List<Article> articles) {
        View view = getView();
        if (view != null) bindNewsList(view, articles);
    }

    @Override
    public void restartMain() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openSalesOrders(String scope) {
        Intent i = new Intent(getContext(), SellerSalesAnalysisActivity.class);
        i.putExtra("sales_scope", scope);
        startActivity(i);
    }

    private void bindNewsList(View root, List<Article> articles) {
        LinearLayout container = root.findViewById(R.id.llNewsContainer);
        if (container == null)
            return;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Article article : articles) {
            if ("农友圈".equals(article.category))
                continue;
            View item = inflater.inflate(R.layout.item_article, container, false);
            bindNewsItem(item, article);
            container.addView(item);
        }
    }

    private void bindNewsItem(View item, Article article) {
        ((TextView) item.findViewById(R.id.tvArticleTitle)).setText(article.title);
        ((TextView) item.findViewById(R.id.tvArticleTime)).setText(article.time);
        TextView category = item.findViewById(R.id.tvArticleCategory);
        if (category != null)
            category.setVisibility(View.GONE);

        ImageView likeIcon = item.findViewById(R.id.ivArticleLike);
        TextView likeCount = item.findViewById(R.id.tvArticleLikeCount);
        TextView commentCount = item.findViewById(R.id.tvArticleCommentCount);
        boolean liked = presenter.isArticleLiked(article.id);
        likeIcon.setImageResource(liked ? R.mipmap.dianzan : R.mipmap.weidianzan);
        likeCount.setText(String.valueOf(presenter.getArticleLikeCount(article.id)));
        commentCount.setText(String.valueOf(presenter.getCommentCount(article.id)));

        View.OnClickListener likeClick = v -> {
            presenter.toggleArticleLike(article.id);
            boolean nowLiked = presenter.isArticleLiked(article.id);
            likeIcon.setImageResource(nowLiked ? R.mipmap.dianzan : R.mipmap.weidianzan);
            likeCount.setText(String.valueOf(presenter.getArticleLikeCount(article.id)));
        };
        likeIcon.setOnClickListener(likeClick);
        likeCount.setOnClickListener(likeClick);

        ImageView thumb = item.findViewById(R.id.ivArticleThumb);
        bindArticleThumb(thumb, article);

        item.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ArticleDetailActivity.class);
            intent.putExtra("article_id", article.id);
            startActivity(intent);
        });
    }

    private void openSellerOrders(String filter) {
        Intent i = new Intent(getContext(), SellerOrdersActivity.class);
        i.putExtra("filter", filter);
        startActivity(i);
    }

    private void bindArticleThumb(ImageView thumb, Article article) {
        if (article.coverUri != null && !article.coverUri.isEmpty()) {
            try {
                thumb.setImageURI(Uri.parse(article.coverUri.split(",")[0]));
                return;
            } catch (Exception ignored) {
            }
        }
        switch (article.id) {
            case 5:
                thumb.setImageResource(R.mipmap.text1);
                break;
            case 4:
                thumb.setImageResource(R.mipmap.text2);
                break;
            case 3:
                thumb.setImageResource(R.mipmap.text3);
                break;
            case 2:
                thumb.setImageResource(R.mipmap.text4);
                break;
            case 1:
                thumb.setImageResource(R.mipmap.text5);
                break;
            default:
                thumb.setImageResource(R.drawable.ic_launcher_background);
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
