package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.AgriCircleAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.mvp.mycircleposts.MyCirclePostsContract;
import com.example.zhinongbao.mvp.mycircleposts.MyCirclePostsPresenter;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【我的动态 / My Circle Posts】View（Activity）
 * 整体逻辑（关键步骤）：
 *   1) onCreate 读取模式，据此设标题、显隐发布 FAB 与「清理失效」入口，初始化列表。
 *   2) onResume 调 presenter.refresh() 拉最新数据。
 *   3) showPosts 用 AgriCircleAdapter 渲染；点赞时在点赞模式下取消点赞会即时从列表移除该行。
 *   4) 「清理失效」弹确认框，确认后调 presenter.clearInvalidPosts() 清掉已被删动态的点赞记录。
 * 数据来源：本类不直接碰数据库，全部经 Presenter 向 ArticleRepository 取数/写入
 *   （Repository 内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 MyCirclePostsContract；业务 MyCirclePostsPresenter；
 *   适配器 adapter/AgriCircleAdapter；布局 activity_my_circle_posts.xml、item_agri_circle_post.xml；
 *   确认弹窗 dialog_confirm.xml；跳转页面 AddCirclePostActivity、ArticleDetailActivity、
 *   SellerStoreActivity；模型 model/Article。
 * 在 MVP 数据流中的位置：View 层（View → Presenter → Repository → ContentProvider → SQLite → 回调 View）。
 * 提示：在 IDE 里搜索「我的动态」可看本组相关文件。
 * ============================================================
 */
public class MyCirclePostsActivity extends BaseMvpActivity<MyCirclePostsContract.Presenter>
        implements MyCirclePostsContract.View {

    private RecyclerView rv;
    private TextView tvEmpty;
    private AgriCircleAdapter adapter;
    private final List<Article> items = new ArrayList<>();
    private boolean favoritesMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_circle_posts);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        favoritesMode = getIntent().getBooleanExtra("circle_favorites", false);
        ((TextView) findViewById(R.id.tvCirclePostsTitle))
                .setText(favoritesMode ? "农友圈点赞" : "我的动态");
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        View fabPost = findViewById(R.id.fabPost);
        fabPost.setVisibility(favoritesMode ? View.GONE : View.VISIBLE);
        fabPost.setOnClickListener(v -> startActivity(new Intent(this, AddCirclePostActivity.class)));

        TextView tvClearInvalid = findViewById(R.id.tvClearInvalid);
        tvClearInvalid.setVisibility(favoritesMode ? View.VISIBLE : View.GONE);
        tvClearInvalid.setOnClickListener(v -> showClearInvalidDialog());

        rv = findViewById(R.id.rvMyPosts);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));

        new MyCirclePostsPresenter(this, this, favoritesMode).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.refresh();
        }
    }

    @Override
    public void showPosts(List<Article> posts, String currentUser) {
        items.clear();
        items.addAll(posts);
        if (adapter == null) {
            adapter = new AgriCircleAdapter(items, currentUser, circleDelegate(),
                new AgriCircleAdapter.OnActionListener() {
                    @Override
                    public void onItemClick(Article article) {
                        Intent i = new Intent(MyCirclePostsActivity.this, ArticleDetailActivity.class);
                        i.putExtra("article_id", article.id);
                        startActivity(i);
                    }
                    @Override
                    public void onLikeClick(Article article, int position) {
                        if (position < 0) {
                            return;
                        }
                        presenter.toggleCircleLike(article.id);
                        if (favoritesMode) {
                            items.remove(position);
                            adapter.notifyItemRemoved(position);
                            boolean empty = items.isEmpty();
                            rv.setVisibility(empty ? View.GONE : View.VISIBLE);
                            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                            if (empty) {
                                tvEmpty.setText("还没有点赞收藏农友圈动态");
                            }
                        } else {
                            adapter.notifyItemChanged(position);
                        }
                    }
                    @Override
                    public void onCommentClick(Article article) {
                        Intent i = new Intent(MyCirclePostsActivity.this, ArticleDetailActivity.class);
                        i.putExtra("article_id", article.id);
                        i.putExtra("focus_comment", true);
                        startActivity(i);
                    }
                    @Override
                    public void onEnterStore(Article article) {
                        Intent i = new Intent(MyCirclePostsActivity.this, SellerStoreActivity.class);
                        i.putExtra("seller", article.author);
                        startActivity(i);
                    }
                    @Override
                    public void onFollow(Article article, int position) {
                        presenter.followUser(article.author);
                        adapter.notifyItemChanged(position);
                    }
                });
            rv.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        boolean empty = items.isEmpty();
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            tvEmpty.setText(favoritesMode
                    ? "还没有点赞收藏农友圈动态"
                    : "您还没有发布过动态\n点右下角 + 发布第一条吧");
        }
    }

    @Override
    public void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void showClearInvalidDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        ((TextView) view.findViewById(R.id.tvDialogTitle)).setText("清理失效动态");
        ((TextView) view.findViewById(R.id.tvDialogMessage))
                .setText("确定要将已删除的动态移出点赞列表吗？");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        view.findViewById(R.id.btnDialogCancel).setOnClickListener(btn -> dialog.dismiss());
        view.findViewById(R.id.btnDialogConfirm).setOnClickListener(btn -> {
            dialog.dismiss();
            presenter.clearInvalidPosts();
        });
        dialog.show();
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
}
