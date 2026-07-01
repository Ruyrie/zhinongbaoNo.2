package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.mvp.search.SearchContract;
import com.example.zhinongbao.mvp.search.SearchPresenter;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【头条搜索 / Search（文章）】View（Activity）
 * 整体逻辑：onCreate 建 Presenter 并 start()；showAllArticles 收到全量文章后
 *   存为 allArticles；输入框 TextWatcher 变化时对 allArticles 本地过滤刷新
 *   results。点结果项进文章详情。搜的是文章，不是商品。
 * 数据来源：文章由 Presenter → ArticleRepository 提供；本类不碰数据库。
 * 配合的文件：接口 SearchContract；业务 SearchPresenter；适配器 adapter/ArticleAdapter；
 *   布局 activity_search.xml；模型 model/Article。
 * 在 MVP 中的位置：View 层。
 * 提示：在 IDE 里搜索「头条搜索」可看本组相关文件。
 * ============================================================
 */
public class SearchActivity extends BaseMvpActivity<SearchContract.Presenter>
        implements SearchContract.View {

    private List<Article>  allArticles;
    private List<Article>  results;
    private ArticleAdapter searchAdapter;

    private RecyclerView  rvResults;
    private LinearLayout  llHome, llNoResults;
    private TextView      tvNoResultsHint;
    private EditText      etQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        allArticles = new ArrayList<>();
        results     = new ArrayList<>();

        // Views
        etQuery               = findViewById(R.id.etSearchQuery);
        TextView    tvCancel  = findViewById(R.id.tvSearchCancel);
        TextView    tvClear   = findViewById(R.id.tvClearSearch);
        rvResults             = findViewById(R.id.rvSearchResults);
        llHome                = findViewById(R.id.llSearchHome);
        llNoResults           = findViewById(R.id.llNoResults);
        tvNoResultsHint       = findViewById(R.id.tvNoResultsHint);

        // RecyclerView
        searchAdapter = new ArticleAdapter(results, article -> {
            Intent i = new Intent(this, ArticleDetailActivity.class);
            i.putExtra("article_id", article.id);
            startActivity(i);
        });
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(searchAdapter);

        // Cancel → dismiss
        tvCancel.setOnClickListener(v -> finish());

        // Clear ✕ button
        tvClear.setOnClickListener(v -> etQuery.setText(""));

        // Live search
        etQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                String q = s.toString().trim();
                tvClear.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                performSearch(q);
            }
        });

        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(etQuery);
                return true;
            }
            return false;
        });

        // Hot-topic tag clicks
        wireTag(R.id.tagSpring, "春耕", etQuery);
        wireTag(R.id.tagFruit, "果树", etQuery);
        wireTag(R.id.tagFresh, "保鲜", etQuery);
        wireTag(R.id.tagStartup, "创业", etQuery);
        wireTag(R.id.tagPlatform, "支农宝", etQuery);
        wireTag(R.id.tagMarket, "农产品", etQuery);

        // Auto-show keyboard
        etQuery.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etQuery, InputMethodManager.SHOW_IMPLICIT);

        new SearchPresenter(this, this).start();
    }

    @Override
    public void showAllArticles(List<Article> articles, String currentUser) {
        allArticles.clear();
        allArticles.addAll(articles);
        searchAdapter = new ArticleAdapter(results, article -> {
            Intent i = new Intent(this, ArticleDetailActivity.class);
            i.putExtra("article_id", article.id);
            startActivity(i);
        }, new ArticleAdapter.ArticleInteractionDelegate() {
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
        rvResults.setAdapter(searchAdapter);
        performSearch(etQuery.getText().toString().trim());
    }

    private void wireTag(int viewId, String query, EditText et) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> {
            et.setText(query);
            et.setSelection(query.length());
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            rvResults.setVisibility(View.GONE);
            llNoResults.setVisibility(View.GONE);
            llHome.setVisibility(View.VISIBLE);
            return;
        }

        llHome.setVisibility(View.GONE);
        results.clear();
        for (Article a : allArticles) {
            if (a.title.contains(query) || a.content.contains(query)
                    || a.author.contains(query)
                    || (a.category != null && a.category.contains(query))) {
                results.add(a);
            }
        }
        searchAdapter.notifyDataSetChanged();

        if (results.isEmpty()) {
            rvResults.setVisibility(View.GONE);
            tvNoResultsHint.setText("未找到\"" + query + "\"的相关内容");
            llNoResults.setVisibility(View.VISIBLE);
        } else {
            rvResults.setVisibility(View.VISIBLE);
            llNoResults.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
