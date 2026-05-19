package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Article;
import java.util.List;

/** 我的文章界面：复用文章列表，仅显示当前账号发布的文章 */
public class MyArticlesActivity extends AppCompatActivity {

    private RecyclerView rv;
    private LinearLayout llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvArticles);
        llEmptyState = findViewById(R.id.llEmptyState);
        TextView btnAddArticle = findViewById(R.id.btnAddArticle);
        TextView ivAddArticleIcon = findViewById(R.id.ivAddArticleIcon);

        View.OnClickListener goAdd = v -> startActivity(new Intent(this, AddArticleActivity.class));
        btnAddArticle.setOnClickListener(goAdd);
        ivAddArticleIcon.setOnClickListener(goAdd);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        DataManager dm = DataManager.getInstance(this);
        String username = dm.getLoggedUser();
        List<Article> articles = dm.getArticlesByAuthor(username);

        if (articles == null || articles.isEmpty()) {
            rv.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new ArticleAdapter(articles, article -> {
                Intent intent = new Intent(this, ArticleDetailActivity.class);
                intent.putExtra("article_id", article.id);
                startActivity(intent);
            }, dm, username));
        }
    }
}
