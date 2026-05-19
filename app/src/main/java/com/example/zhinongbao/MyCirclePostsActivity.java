package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.AgriCircleAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Article;
import java.util.ArrayList;
import java.util.List;

public class MyCirclePostsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvEmpty;
    private AgriCircleAdapter adapter;
    private final List<Article> items = new ArrayList<>();
    private DataManager dm;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_circle_posts);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dm = DataManager.getInstance(this);
        currentUser = dm.getLoggedUser();

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabPost).setOnClickListener(v ->
                startActivity(new Intent(this, AddCirclePostActivity.class)));

        rv = findViewById(R.id.rvMyPosts);
        tvEmpty = findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AgriCircleAdapter(items, currentUser, dm,
                new AgriCircleAdapter.OnActionListener() {
                    @Override
                    public void onItemClick(Article article) {
                        Intent i = new Intent(MyCirclePostsActivity.this, ArticleDetailActivity.class);
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
                        dm.followUser(currentUser, article.author);
                        adapter.notifyItemChanged(position);
                    }
                });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        List<Article> fresh = dm.getCirclePostsByAuthor(currentUser);
        items.clear();
        items.addAll(fresh);
        if (adapter != null) adapter.notifyDataSetChanged();
        boolean empty = items.isEmpty();
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
