package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.mvp.myfavorites.MyFavoritesContract;
import com.example.zhinongbao.mvp.myfavorites.MyFavoritesPresenter;
import java.util.List;

/** 我的收藏：展示当前用户点赞（收藏）的文章 */
public class MyFavoritesActivity extends BaseMvpActivity<MyFavoritesContract.Presenter>
        implements MyFavoritesContract.View {

    private RecyclerView rv;
    private ArticleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_favorites);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        findViewById(R.id.tvFavBack).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvArticles);
        rv.setLayoutManager(new LinearLayoutManager(this));

        new MyFavoritesPresenter(this, this).start();

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
                presenter.clearInvalidArticles();
            });
            dialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.refresh();
        }
    }

    @Override
    public void showArticles(List<Article> articles, String currentUser) {
        adapter = new ArticleAdapter(articles, article -> openArticleDetail(article.id),
                new ArticleAdapter.ArticleInteractionDelegate() {
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
                    public void toggleArticleLike(int articleId) {
                        presenter.toggleArticleLike(articleId);
                    }
                }, currentUser);
        rv.setAdapter(adapter);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openArticleDetail(int articleId) {
        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_id", articleId);
        startActivity(intent);
    }
}
