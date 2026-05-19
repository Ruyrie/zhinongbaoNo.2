package com.example.zhinongbao;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Article;
import java.util.ArrayList;
import java.util.List;

/** iOS 风格全屏搜索界面 */
public class SearchActivity extends AppCompatActivity {

    private List<Article>  allArticles;
    private List<Article>  results;
    private ArticleAdapter searchAdapter;

    private RecyclerView  rvResults;
    private LinearLayout  llHome, llNoResults;
    private TextView      tvNoResultsHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        DataManager dm          = DataManager.getInstance(this);
        String      currentUser = dm.getLoggedUser();
        allArticles = dm.getArticles();
        results     = new ArrayList<>();

        // Views
        EditText    etQuery   = findViewById(R.id.etSearchQuery);
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
        }, dm, currentUser);
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
        wireTag(R.id.tagRice,    "水稻",   etQuery);
        wireTag(R.id.tagSmart,   "智慧农业", etQuery);
        wireTag(R.id.tagRural,   "乡村振兴", etQuery);
        wireTag(R.id.tagEco,     "农业经济", etQuery);
        wireTag(R.id.tagTech,    "科技节",  etQuery);
        wireTag(R.id.tagOrganic, "有机",   etQuery);

        // Auto-show keyboard
        etQuery.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etQuery, InputMethodManager.SHOW_IMPLICIT);
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
