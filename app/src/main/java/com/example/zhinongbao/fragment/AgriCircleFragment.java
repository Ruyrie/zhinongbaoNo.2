package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.AddCirclePostActivity;
import com.example.zhinongbao.ArticleDetailActivity;
import com.example.zhinongbao.MyCirclePostsActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.SellerStoreActivity;
import com.example.zhinongbao.adapter.AgriCircleAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Article;
import java.util.ArrayList;
import java.util.List;

public class AgriCircleFragment extends Fragment {

    private static final int TAB_FOLLOW = 0;
    private static final int TAB_LATEST = 1;
    private static final int TAB_MINE   = 2; // 由头部"我的"触发，不在 tab 栏显示

    private DataManager dm;
    private String currentUser;
    private RecyclerView rvCircle;
    private TextView tvEmpty;
    private AgriCircleAdapter adapter;
    private final List<Article> items = new ArrayList<>();
    private int currentTab = TAB_LATEST;

    private View tabFollow, tabLatest;
    private TextView tvTabFollow, tvTabLatest;
    private View tabIndicator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agricircle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        dm = DataManager.getInstance(requireContext());
        currentUser = dm.getLoggedUser();

        rvCircle = view.findViewById(R.id.rvCircle);
        tvEmpty  = view.findViewById(R.id.tvCircleEmpty);
        rvCircle.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabFollow    = view.findViewById(R.id.tabFollow);
        tabLatest    = view.findViewById(R.id.tabLatest);
        tvTabFollow  = view.findViewById(R.id.tvTabFollow);
        tvTabLatest  = view.findViewById(R.id.tvTabLatest);
        tabIndicator = view.findViewById(R.id.tabIndicator);

        // 等布局完成后初始化指示器
        tabFollow.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        tabFollow.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        selectTab(TAB_LATEST, false);
                    }
                });

        tabFollow.setOnClickListener(v -> selectTab(TAB_FOLLOW, true));
        tabLatest.setOnClickListener(v -> selectTab(TAB_LATEST, true));

        // 头部"我的"：跳转到独立的"我的动态"页面
        view.findViewById(R.id.llHeaderMine).setOnClickListener(v ->
                startActivity(new Intent(getContext(), MyCirclePostsActivity.class)));

        // 加载头像
        bindHeaderAvatar(view);

        // FAB：发农友圈
        view.findViewById(R.id.fabCirclePost).setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddCirclePostActivity.class)));

        // Adapter
        adapter = new AgriCircleAdapter(items, currentUser, dm,
                new AgriCircleAdapter.OnActionListener() {
                    @Override
                    public void onItemClick(Article article) {
                        Intent i = new Intent(getContext(), ArticleDetailActivity.class);
                        i.putExtra("article_id", article.id);
                        startActivity(i);
                    }
                    @Override
                    public void onLikeClick(Article article, int position) {
                        if (dm.isArticleLiked(currentUser, article.id)) {
                            dm.unlikeArticle(currentUser, article.id);
                        } else {
                            dm.likeArticle(currentUser, article.id);
                        }
                        adapter.notifyItemChanged(position);
                    }
                    @Override
                    public void onCommentClick(Article article) {
                        Intent i = new Intent(getContext(), ArticleDetailActivity.class);
                        i.putExtra("article_id", article.id);
                        i.putExtra("focus_comment", true);
                        startActivity(i);
                    }
                    @Override
                    public void onEnterStore(Article article) {
                        Intent i = new Intent(getContext(), SellerStoreActivity.class);
                        i.putExtra("seller", article.author);
                        startActivity(i);
                    }
                    @Override
                    public void onFollow(Article article, int position) {
                        dm.followUser(currentUser, article.author);
                        adapter.notifyItemChanged(position);
                    }
                });
        rvCircle.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dm != null) loadPosts();
    }

    // ── 选中关注/最新 tab ──
    private void selectTab(int tab, boolean animate) {
        currentTab = tab;

        // 样式：两个 tab 正常显示
        tvTabFollow.setTypeface(null, tab == TAB_FOLLOW ? Typeface.BOLD : Typeface.NORMAL);
        tvTabFollow.setTextColor(tab == TAB_FOLLOW ? 0xFF1A1A1A : 0xFFAAAAAA);
        tvTabLatest.setTypeface(null, tab == TAB_LATEST ? Typeface.BOLD : Typeface.NORMAL);
        tvTabLatest.setTextColor(tab == TAB_LATEST ? 0xFF1A1A1A : 0xFFAAAAAA);

        // 指示器显示并移动
        tabIndicator.setVisibility(View.VISIBLE);
        tabFollow.post(() -> {
            int tabW = tabFollow.getWidth();
            android.view.ViewGroup.LayoutParams lp = tabIndicator.getLayoutParams();
            lp.width = tabW;
            tabIndicator.setLayoutParams(lp);
            float targetX = tab == TAB_FOLLOW ? 0f : tabW;
            if (animate) {
                tabIndicator.animate().translationX(targetX).setDuration(200).start();
            } else {
                tabIndicator.setTranslationX(targetX);
            }
        });

        loadPosts();
    }

    // ── "我的"模式：指示器隐藏，两个 tab 均置灰 ──
    private void selectMine() {
        currentTab = TAB_MINE;
        tvTabFollow.setTypeface(null, Typeface.NORMAL);
        tvTabFollow.setTextColor(0xFFAAAAAA);
        tvTabLatest.setTypeface(null, Typeface.NORMAL);
        tvTabLatest.setTextColor(0xFFAAAAAA);
        tabIndicator.setVisibility(View.INVISIBLE);
        loadPosts();
    }

    private void loadPosts() {
        List<Article> fresh;
        switch (currentTab) {
            case TAB_FOLLOW:
                fresh = dm.getCirclePostsByFollowing(currentUser);
                break;
            case TAB_MINE:
                fresh = dm.getCirclePostsByAuthor(currentUser);
                break;
            default:
                fresh = dm.getCirclePosts();
                break;
        }
        items.clear();
        items.addAll(fresh);
        adapter.notifyDataSetChanged();

        boolean empty = items.isEmpty();
        rvCircle.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            switch (currentTab) {
                case TAB_FOLLOW:
                    tvEmpty.setText("还没有关注的农友发布动态\n去找有趣的农友关注吧 👥");
                    break;
                case TAB_MINE:
                    tvEmpty.setText("您还没有发布过动态\n点右下角 + 发布第一条吧 🌾");
                    break;
                default:
                    tvEmpty.setText("这里还没有动态\n快来发布第一条吧 🌾");
                    break;
            }
        }
    }

    private void bindHeaderAvatar(View view) {
        ImageView ivAvatar = view.findViewById(R.id.ivHeaderAvatar);
        TextView tvInitial = view.findViewById(R.id.tvHeaderAvatarInitial);
        String avatarUri = dm.getAvatarUri(currentUser);
        if (avatarUri != null && !avatarUri.isEmpty()) {
            try {
                if (avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils
                            .setAvatarFromBase64(ivAvatar, avatarUri);
                } else {
                    ivAvatar.setImageURI(Uri.parse(avatarUri));
                }
                ivAvatar.setVisibility(View.VISIBLE);
                tvInitial.setVisibility(View.GONE);
                return;
            } catch (Exception ignored) {}
        }
        ivAvatar.setVisibility(View.GONE);
        tvInitial.setVisibility(View.VISIBLE);
        tvInitial.setText(currentUser.isEmpty() ? "我" :
                String.valueOf(currentUser.charAt(0)).toUpperCase());
    }
}
