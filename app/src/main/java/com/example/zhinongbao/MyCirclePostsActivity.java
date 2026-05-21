package com.example.zhinongbao;

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

public class MyCirclePostsActivity extends BaseMvpActivity<MyCirclePostsContract.Presenter>
        implements MyCirclePostsContract.View {

    private RecyclerView rv;
    private TextView tvEmpty;
    private AgriCircleAdapter adapter;
    private final List<Article> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_circle_posts);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabPost).setOnClickListener(v ->
                startActivity(new Intent(this, AddCirclePostActivity.class)));

        rv = findViewById(R.id.rvMyPosts);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));

        new MyCirclePostsPresenter(this, this).start();
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
                        presenter.toggleArticleLike(article.id);
                        adapter.notifyItemChanged(position);
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
    }

    private AgriCircleAdapter.CircleInteractionDelegate circleDelegate() {
        return new AgriCircleAdapter.CircleInteractionDelegate() {
            @Override
            public int getArticleLikeCount(int articleId) {
                return presenter.getArticleLikeCount(articleId);
            }

            @Override
            public int getCommentCount(int articleId) {
                return presenter.getCommentCount(articleId);
            }

            @Override
            public boolean isArticleLiked(int articleId) {
                return presenter.isArticleLiked(articleId);
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
