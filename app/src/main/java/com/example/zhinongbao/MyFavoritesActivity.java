package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Article;
import java.util.List;

/** 我的收藏：展示当前用户点赞（收藏）的文章 */
public class MyFavoritesActivity extends AppCompatActivity {

    private DataManager dm;
    private String username;
    private RecyclerView rv;
    private ArticleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_favorites);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        findViewById(R.id.tvFavBack).setOnClickListener(v -> finish());

        dm = DataManager.getInstance(this);
        username = dm.getLoggedUser();

        rv = findViewById(R.id.rvArticles);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadData();

        findViewById(R.id.tvClearInvalid).setOnClickListener(v -> {
            android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
            android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
            android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
            tvTitle.setText("清理失效文章");
            tvMessage.setText("确定要将已删除的文章移出收藏列表吗？");

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            view.findViewById(R.id.btnDialogCancel).setOnClickListener(btn -> dialog.dismiss());
            view.findViewById(R.id.btnDialogConfirm).setOnClickListener(btn -> {
                dialog.dismiss();
                int cleared = dm.clearInvalidLikedArticles(username);
                if (cleared > 0) {
                    Toast.makeText(this, "成功清理 " + cleared + " 篇失效文章", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(this, "没有需要清理的失效文章", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        List<Article> articles = dm.getLikedArticles(username);
        adapter = new ArticleAdapter(articles, article -> {
            Intent intent = new Intent(this, ArticleDetailActivity.class);
            intent.putExtra("article_id", article.id);
            startActivity(intent);
        }, dm, username);
        rv.setAdapter(adapter);
    }
}
