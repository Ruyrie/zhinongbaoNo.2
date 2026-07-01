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

/**
 * ============================================================
 * 【我的文章 / My Articles】View（Activity）
 * 整体逻辑（关键步骤）：
 *   1) onCreate 读取 targetAuthor，初始化列表与「发文章」入口，创建 Presenter start()。
 *   2) onResume 调 presenter.refresh() 拉最新数据。
 *   3) showArticles 判断是不是看自己(viewingSelf)：据此设标题、显隐发文入口与空态文案，
 *      再用 ArticleAdapter 渲染列表并注入点赞交互委托。
 *   4) 点击某条调 openArticleDetail 进详情页。
 * 数据来源：本类不直接碰数据库，全部经 Presenter 向 ArticleRepository 取数
 *   （Repository 内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 MyArticlesContract；业务 MyArticlesPresenter；
 *   适配器 adapter/ArticleAdapter；布局 activity_article_list.xml、item_article.xml；
 *   跳转页面 AddArticleActivity（发文）、ArticleDetailActivity（详情）；模型 model/Article。
 * 在 MVP 数据流中的位置：View 层（View → Presenter → Repository → ContentProvider → SQLite → 回调 View）。
 * 提示：在 IDE 里搜索「我的文章」可看本组相关文件。
 * ============================================================
 */
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
