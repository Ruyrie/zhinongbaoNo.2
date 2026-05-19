package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.AddArticleActivity;
import com.example.zhinongbao.ArticleDetailActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.SearchActivity;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Article;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class HeadlineFragment extends Fragment {

    private static final String[] CATEGORIES = { "热点新闻", "专家咨询", "支农宝新闻", "创业项目" };

    private List<Article> allArticles;
    private List<Article> displayed;
    private ArticleAdapter adapter;
    private String selectedCategory = "热点新闻";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_headline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        DataManager dm = DataManager.getInstance(requireContext());
        allArticles = dm.getArticles();
        displayed = new ArrayList<>(allArticles);

        RecyclerView rv = view.findViewById(R.id.rvArticles);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticleAdapter(displayed, article -> {
            Intent intent = new Intent(getContext(), ArticleDetailActivity.class);
            intent.putExtra("article_id", article.id);
            startActivity(intent);
        }, dm, dm.getLoggedUser());
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnOpenSearch)
                .setOnClickListener(v -> startActivity(new Intent(getContext(), SearchActivity.class)));

        view.findViewById(R.id.fabAddArticle)
                .setOnClickListener(v -> startActivity(new Intent(getContext(), AddArticleActivity.class)));

        setupCategoryTabs(view);
    }

    private void setupCategoryTabs(View view) {
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);

        for (String cat : CATEGORIES) {
            tabLayout.addTab(tabLayout.newTab().setText(cat));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() != null) {
                    selectedCategory = tab.getText().toString();
                    filterByCategory();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        filterByCategory();
    }

    private void filterByCategory() {
        displayed.clear();
        for (Article a : allArticles) {
            if ("全部".equals(selectedCategory) || selectedCategory.equals(a.category)) {
                displayed.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            DataManager dm = DataManager.getInstance(requireContext());
            allArticles.clear();
            allArticles.addAll(dm.getArticles());
            filterByCategory();
        }
    }
}
