package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.zhinongbao.activity.ProductDetailActivity;
import com.example.zhinongbao.R;
import com.example.zhinongbao.adapter.ProductAdapter;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.mvp.mall.MallContract;
import com.example.zhinongbao.mvp.mall.MallPresenter;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class MallFragment extends BaseMvpFragment<MallContract.Presenter>
        implements MallContract.View {

    private static final String[] CATEGORIES = { "推荐", "水果蔬菜", "米面粮油", "农资农具" };

    private List<Product> allProducts;
    private List<Product> displayed;
    private ProductAdapter adapter;
    private String selectedCategory = "推荐";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mall, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        allProducts = new ArrayList<>();
        displayed = new ArrayList<>();

        RecyclerView rv = view.findViewById(R.id.rvProducts);
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        rv.setLayoutManager(layoutManager);

        TextView btnScrollTop = view.findViewById(R.id.btnScrollTop);
        if (btnScrollTop != null) {
            btnScrollTop.setOnClickListener(v -> {
                rv.stopScroll();
                layoutManager.invalidateSpanAssignments();
                layoutManager.scrollToPositionWithOffset(0, 0);
                btnScrollTop.setVisibility(View.GONE);
            });
            rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int offset = recyclerView.computeVerticalScrollOffset();
                    if (offset == 0) {
                        layoutManager.invalidateSpanAssignments();
                    }
                    boolean show = offset > recyclerView.getHeight();
                    btnScrollTop.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }

        adapter = new ProductAdapter(displayed, product -> {
            Intent intent = new Intent(getContext(), ProductDetailActivity.class);
            intent.putExtra("product_id", product.id);
            startActivity(intent);
        });
        adapter.setOnAddCartListener(product -> {
            presenter.addToCart(product);
        });
        rv.setAdapter(adapter);

        // 搜索框点击进入搜索页面
        EditText etSearch = view.findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), com.example.zhinongbao.activity.ProductSearchActivity.class));
            });
        }

        // 去卖货按钮
        View btnGoSell = view.findViewById(R.id.btnGoSell);
        if (btnGoSell != null) {
            btnGoSell.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), com.example.zhinongbao.activity.AddProductActivity.class));
            });
        }

        // 设置 TabLayout
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        if (tabLayout != null) {
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
        }

        filterByCategory();
        new MallPresenter(requireContext(), this).start();
    }

    @Override
    public void showProducts(List<Product> products) {
        allProducts.clear();
        allProducts.addAll(products);
        filterByCategory();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void filterByCategory() {
        displayed.clear();
        for (Product p : allProducts) {
            if (p.category != null && p.category.contains(selectedCategory)) {
                displayed.add(p);
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
