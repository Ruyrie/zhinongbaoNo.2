package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.zhinongbao.adapter.ProductAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.mvp.productfavorites.ProductFavoritesContract;
import com.example.zhinongbao.mvp.productfavorites.ProductFavoritesPresenter;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【商品收藏 / Product Favorites】View（界面/Activity）
 * 整体逻辑：onCreate 装好瀑布流 RecyclerView 与 ProductAdapter（紧凑模式），
 *   创建 Presenter 并 start()；Presenter 查好收藏商品后回调 showProducts 渲染，
 *   并根据是否为空切换列表/空提示的显示。onResume 时刷新，保证取消收藏后返回同步。
 * 数据来源：不直接碰数据库；经 Presenter 走 repository/ProductRepository，
 *   Repository 内部通过 ContentProvider 访问 SQLite。
 * 配合的文件：接口约定 mvp/productfavorites/ProductFavoritesContract；业务逻辑
 *   ProductFavoritesPresenter；列表适配器 adapter/ProductAdapter；页面布局
 *   res/layout/activity_product_favorites.xml；模型 model/Product；
 *   会跳转到商品详情 ProductDetailActivity。
 * 在 MVP 数据流中的位置：View 层。
 * 提示：在 IDE 里搜索「商品收藏」可看本组相关文件。
 * ============================================================
 */
public class ProductFavoritesActivity extends BaseMvpActivity<ProductFavoritesContract.Presenter>
        implements ProductFavoritesContract.View {
    private final List<Product> products = new ArrayList<>(); // 收藏商品数据源
    private ProductAdapter adapter;                           // 商品列表适配器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_favorites);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        findViewById(R.id.tvProductFavBack).setOnClickListener(v -> finish());

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvProductFavorites);
        rv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new ProductAdapter(products, product -> {
            Intent i = new Intent(this, ProductDetailActivity.class);
            i.putExtra("product_id", product.id);
            startActivity(i);
        });
        adapter.setCompactMode(true);
        rv.setAdapter(adapter);

        new ProductFavoritesPresenter(this, this).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.refresh();
        }
    }

    @Override
    public void showProducts(List<Product> favoriteProducts) {
        products.clear();
        products.addAll(favoriteProducts);
        if (adapter != null) adapter.notifyDataSetChanged();
        TextView empty = findViewById(R.id.tvProductFavEmpty);
        View rv = findViewById(R.id.rvProductFavorites);
        boolean isEmpty = products.isEmpty();
        rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
