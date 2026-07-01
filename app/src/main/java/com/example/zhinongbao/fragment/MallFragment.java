package com.example.zhinongbao.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【商城首页 / Mall】View（Fragment）
 * 整体逻辑：onViewCreated 初始化瀑布流 RecyclerView 与 ProductAdapter，
 *   绑定各入口点击；创建 MallPresenter 并 start() 拉商品；showProducts 收到
 *   全量数据后按当前分类 filterByCategory() 本地过滤展示。点商品项跳
 *   ProductDetailActivity；点加购调 presenter.addToCart()。onResume 会 refresh。
 * 数据来源：商品数据来自 Presenter → ProductRepository → SQLite；
 *   部分图标(如回到顶部)来自 assets/pic/ 本地图片。
 * 配合的文件：接口 MallContract；业务 MallPresenter；适配器 adapter/ProductAdapter；
 *   布局 fragment_mall.xml；跳转 ProductDetailActivity / ProductSearchActivity /
 *   AddProductActivity；模型 model/Product。
 * 在 MVP 中的位置：View 层。
 * 提示：在 IDE 里搜索「商城」可看本组相关文件。
 * ============================================================
 */
public class MallFragment extends BaseMvpFragment<MallContract.Presenter>
        implements MallContract.View {

    private static final String[] CATEGORIES = { "推荐", "水果蔬菜", "米面粮油", "农资农具" };

    private List<Product> allProducts;
    private List<Product> displayed;
    private ProductAdapter adapter;
    private RecyclerView rvProducts;
    private StaggeredGridLayoutManager productLayoutManager;
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

        rvProducts = view.findViewById(R.id.rvProducts);
        productLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        productLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        rvProducts.setLayoutManager(productLayoutManager);

        ImageView btnScrollTop = view.findViewById(R.id.btnScrollTop);
        if (btnScrollTop != null) {
            loadAssetImage(btnScrollTop, "xiangshangfanhui.png");
            btnScrollTop.setOnClickListener(v -> {
                rvProducts.stopScroll();
                productLayoutManager.invalidateSpanAssignments();
                productLayoutManager.scrollToPositionWithOffset(0, 0);
                btnScrollTop.setVisibility(View.GONE);
            });
            rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int offset = recyclerView.computeVerticalScrollOffset();
                    if (offset == 0) {
                        productLayoutManager.invalidateSpanAssignments();
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
        rvProducts.setAdapter(adapter);

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

    // 按当前选中分类本地过滤：「推荐」显示全部，其它分类按 category 包含匹配
    private void filterByCategory() {
        displayed.clear();
        for (Product p : allProducts) {
            if ("推荐".equals(selectedCategory)
                    || (p.category != null && p.category.contains(selectedCategory))) {
                displayed.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        resetProductListPosition(); // 切换分类后回到列表顶部
    }

    private void resetProductListPosition() {
        if (rvProducts == null || productLayoutManager == null) {
            return;
        }
        rvProducts.stopScroll();
        productLayoutManager.invalidateSpanAssignments();
        productLayoutManager.scrollToPositionWithOffset(0, 0);
        rvProducts.post(() -> {
            productLayoutManager.invalidateSpanAssignments();
            productLayoutManager.scrollToPositionWithOffset(0, 0);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.refresh();
        }
    }

    private void loadAssetImage(ImageView iv, String filename) {
        try (InputStream is = requireContext().getAssets().open("pic/" + filename)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            iv.setImageBitmap(bmp);
        } catch (IOException ignored) {
        }
    }
}
