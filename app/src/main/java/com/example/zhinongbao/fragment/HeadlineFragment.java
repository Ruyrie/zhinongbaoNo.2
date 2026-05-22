package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.activity.AddArticleActivity;
import com.example.zhinongbao.activity.ArticleDetailActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.activity.SearchActivity;
import com.example.zhinongbao.adapter.ArticleAdapter;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.mvp.headline.HeadlineContract;
import com.example.zhinongbao.mvp.headline.HeadlinePresenter;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class HeadlineFragment extends BaseMvpFragment<HeadlineContract.Presenter>
        implements HeadlineContract.View {

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
        allArticles = new ArrayList<>();
        displayed = new ArrayList<>();

        RecyclerView rv = view.findViewById(R.id.rvArticles);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticleAdapter(displayed, article -> {
            Intent intent = new Intent(getContext(), ArticleDetailActivity.class);
            intent.putExtra("article_id", article.id);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnOpenSearch)
                .setOnClickListener(v -> startActivity(new Intent(getContext(), SearchActivity.class)));

        view.findViewById(R.id.fabAddArticle)
                .setOnClickListener(v -> startActivity(new Intent(getContext(), AddArticleActivity.class)));

        setupCategoryTabs(view);
        new HeadlinePresenter(requireContext(), this).start();
    }

    @Override
    public void showArticles(List<Article> articles, String currentUser) {
        allArticles.clear();
        allArticles.addAll(articles);
        adapter = new ArticleAdapter(displayed, article -> {
            Intent intent = new Intent(getContext(), ArticleDetailActivity.class);
            intent.putExtra("article_id", article.id);
            startActivity(intent);
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
        RecyclerView rv = requireView().findViewById(R.id.rvArticles);
        rv.setAdapter(adapter);
        filterByCategory();
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
        if (presenter != null) {
            presenter.refresh();
        }
    }
}
