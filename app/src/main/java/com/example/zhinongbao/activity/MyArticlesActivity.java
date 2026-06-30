package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.mvp.myarticles.MyArticlesContract;
import com.example.zhinongbao.mvp.myarticles.MyArticlesPresenter;
import java.util.List;

/** 我的文章界面：复用文章列表，仅显示当前账号发布的文章 */
public class MyArticlesActivity extends BaseMvpActivity<MyArticlesContract.Presenter>
        implements MyArticlesContract.View {

    private RecyclerView rv;
    private LinearLayout llEmptyState;
    private String targetAuthor;
    private boolean viewingSelf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        targetAuthor = getIntent().getStringExtra("author");

        rv = findViewById(R.id.rvArticles);
        llEmptyState = findViewById(R.id.llEmptyState);
        TextView btnAddArticle = findViewById(R.id.btnAddArticle);
        TextView ivAddArticleIcon = findViewById(R.id.ivAddArticleIcon);

        View.OnClickListener goAdd = v -> startActivity(new Intent(this, AddArticleActivity.class));
        btnAddArticle.setOnClickListener(goAdd);
        ivAddArticleIcon.setOnClickListener(goAdd);

        new MyArticlesPresenter(this, this, targetAuthor).start();
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
        viewingSelf = targetAuthor == null || targetAuthor.isEmpty() || targetAuthor.equals(currentUser);
        TextView title = findViewById(R.id.tvArticleListTitle);
        title.setText(viewingSelf ? "我的文章" : targetAuthor + "的文章");
        findViewById(R.id.ivAddArticleIcon).setVisibility(viewingSelf ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.btnAddArticle).setVisibility(viewingSelf ? View.VISIBLE : View.GONE);
        if (articles == null || articles.isEmpty()) {
            // 看自己的列表用「您」，看别人的列表用其名字，避免对访客显示「您还未上传过文章」
            TextView tvEmpty = findViewById(R.id.tvEmptyArticleText);
            tvEmpty.setText(viewingSelf ? "您还未上传过文章" : targetAuthor + " 还未上传过文章");
            rv.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new ArticleAdapter(articles, article -> openArticleDetail(article.id),
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
                    }, currentUser));
        }
    }

    @Override
    public void openArticleDetail(int articleId) {
        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra("article_id", articleId);
        startActivity(intent);
    }
}
