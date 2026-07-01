package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.activity.AddCirclePostActivity;
import com.example.zhinongbao.activity.ArticleDetailActivity;
import com.example.zhinongbao.activity.MyCirclePostsActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.activity.SellerStoreActivity;
import com.example.zhinongbao.adapter.AgriCircleAdapter;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.mvp.agricircle.AgriCircleContract;
import com.example.zhinongbao.mvp.agricircle.AgriCirclePresenter;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;

/**
 * ============================================================
 * 【农友圈 / Agri Circle】View（Fragment）
 * 整体逻辑（关键步骤）：
 *   1) onViewCreated 里初始化 RecyclerView、顶部「关注/最新」两个 tab 和滑动指示器、
 *      「我的」入口、发布 FAB，然后创建 Presenter 并 start()。
 *   2) selectTab 切换「关注」或「最新」时调 loadPosts()，按当前 tab 调用
 *      presenter.loadFollowing() / loadLatest() / loadMine() 取对应动态。
 *   3) showPosts 回调把数据交给 AgriCircleAdapter 渲染；空列表时按 tab 显示不同空态文案。
 *   4) 点击某条进文章详情页；点赞/关注即时刷新对应行。
 * 数据来源：本类不直接碰数据库，全部经 Presenter 向 ArticleRepository 取数
 *   （Repository 内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 AgriCircleContract；业务 AgriCirclePresenter；
 *   适配器 adapter/AgriCircleAdapter；布局 fragment_agricircle.xml、item_agri_circle_post.xml；
 *   跳转页面 AddCirclePostActivity（发动态）、MyCirclePostsActivity（我的动态）、
 *   ArticleDetailActivity（详情）、SellerStoreActivity（进店）；模型 model/Article。
 * 在 MVP 数据流中的位置：View 层（View → Presenter → Repository → ContentProvider → SQLite → 回调 View）。
 * 提示：在 IDE 里搜索「农友圈」可看本组相关文件。
 * ============================================================
 */
public class AgriCircleFragment extends BaseMvpFragment<AgriCircleContract.Presenter>
        implements AgriCircleContract.View {

    private static final int TAB_FOLLOW = 0;
    private static final int TAB_LATEST = 1;
    private static final int TAB_MINE   = 2; // 由头部"我的"触发，不在 tab 栏显示

    private String currentUser;
    private RecyclerView rvCircle;
    private TextView tvEmpty;
    private AgriCircleAdapter adapter;
    private final List<Article> items = new ArrayList<>();
    private int currentTab = TAB_LATEST;

    private View tabFollow, tabLatest;
    private TextView tvTabFollow, tvTabLatest;
    private View tabIndicator;
    private ImageView btnScrollTop;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agricircle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvCircle = view.findViewById(R.id.rvCircle);
        tvEmpty  = view.findViewById(R.id.tvCircleEmpty);
        rvCircle.setLayoutManager(new LinearLayoutManager(requireContext()));
        btnScrollTop = view.findViewById(R.id.btnCircleScrollTop);
        loadScrollTopIcon(btnScrollTop);
        btnScrollTop.setOnClickListener(v -> rvCircle.smoothScrollToPosition(0));
        rvCircle.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                btnScrollTop.setVisibility(recyclerView.canScrollVertically(-1) ? View.VISIBLE : View.GONE);
            }
        });

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

        // FAB：发农友圈
        view.findViewById(R.id.fabCirclePost).setOnClickListener(v ->
                startActivity(new Intent(getContext(), AddCirclePostActivity.class)));

        new AgriCirclePresenter(requireContext(), this).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (presenter != null) loadPosts();
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
        switch (currentTab) {
            case TAB_FOLLOW:
                presenter.loadFollowing();
                break;
            case TAB_MINE:
                presenter.loadMine();
                break;
            default:
                presenter.loadLatest();
                break;
        }
    }

    @Override
    public void showPosts(List<Article> posts, String currentUser) {
        this.currentUser = currentUser;
        items.clear();
        items.addAll(posts);
        if (adapter == null) {
            adapter = new AgriCircleAdapter(items, currentUser, circleDelegate(), circleActionListener());
            rvCircle.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

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

    @Override
    public void showHeaderAvatar(String currentUser, String avatarUri) {
        this.currentUser = currentUser == null ? "" : currentUser;
        View view = requireView();
        ImageView ivAvatar = view.findViewById(R.id.ivHeaderAvatar);
        TextView tvInitial = view.findViewById(R.id.tvHeaderAvatarInitial);
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
        tvInitial.setText(this.currentUser.isEmpty() ? "我" :
                String.valueOf(this.currentUser.charAt(0)).toUpperCase());
    }

    private AgriCircleAdapter.CircleInteractionDelegate circleDelegate() {
        return new AgriCircleAdapter.CircleInteractionDelegate() {
            @Override
            public int getCircleLikeCount(int articleId) {
                return presenter.getCircleLikeCount(articleId);
            }

            @Override
            public int getCommentCount(int articleId) {
                return presenter.getCommentCount(articleId);
            }

            @Override
            public boolean isCircleLiked(int articleId) {
                return presenter.isCircleLiked(articleId);
            }

            @Override
            public boolean isFollowing(String author) {
                return presenter.isFollowing(author);
            }

            @Override
            public int getUserRole(String username) {
                return presenter.getUserRole(username);
            }
        };
    }

    private AgriCircleAdapter.OnActionListener circleActionListener() {
        return new AgriCircleAdapter.OnActionListener() {
            @Override
            public void onItemClick(Article article) {
                Intent i = new Intent(getContext(), ArticleDetailActivity.class);
                i.putExtra("article_id", article.id);
                startActivity(i);
            }

            @Override
            public void onLikeClick(Article article, int position) {
                if (position < 0) {
                    return;
                }
                presenter.toggleCircleLike(article.id);
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
                if (position < 0) {
                    return;
                }
                presenter.followUser(article.author);
                adapter.notifyItemChanged(position);
            }
        };
    }

    private void loadScrollTopIcon(ImageView iv) {
        try (InputStream is = requireContext().getAssets().open("pic/xiangshangfanhui.png")) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            iv.setImageBitmap(bitmap);
        } catch (Exception ignored) {
            iv.setImageResource(R.mipmap.fanhui);
        }
    }
}
