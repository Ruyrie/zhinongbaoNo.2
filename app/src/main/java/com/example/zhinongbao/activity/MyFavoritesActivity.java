package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
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

/**
 * ============================================================
 * 【我的收藏 / My Favorites】View（界面/Activity）
 * 整体逻辑：onCreate 装好 RecyclerView，创建 Presenter 并 start()；Presenter
 *   查好收藏文章后回调 showArticles 用 ArticleAdapter 渲染，适配器通过回调向
 *   Presenter 询问点赞数/评论数/是否已赞并触发点赞切换；「清理失效」按钮弹确认框，
 *   确认后调 presenter.clearInvalidArticles。onResume 时刷新保持同步。
 * 数据来源：不直接碰数据库；经 Presenter 走 repository/ArticleRepository，
 *   Repository 内部通过 ContentProvider 访问 SQLite。
 * 配合的文件：接口约定 mvp/myfavorites/MyFavoritesContract；业务逻辑
 *   MyFavoritesPresenter；列表适配器 adapter/ArticleAdapter；页面布局
 *   res/layout/activity_my_favorites.xml；确认弹窗 res/layout/dialog_confirm.xml；
 *   模型 model/Article；会跳转到文章详情 ArticleDetailActivity。
 * 在 MVP 数据流中的位置：View 层。
 * 提示：在 IDE 里搜索「我的收藏」可看本组相关文件。
 * ============================================================
 */
public class MyFavoritesActivity extends BaseMvpActivity<MyFavoritesContract.Presenter>
        implements MyFavoritesContract.View {

    private RecyclerView rv;              // 收藏文章列表
    private ArticleAdapter adapter;      // 文章列表适配器

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
