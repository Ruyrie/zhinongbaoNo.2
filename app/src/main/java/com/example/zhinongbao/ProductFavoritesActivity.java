package com.example.zhinongbao;

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

public class ProductFavoritesActivity extends BaseMvpActivity<ProductFavoritesContract.Presenter>
        implements ProductFavoritesContract.View {
    private final List<Product> products = new ArrayList<>();
    private ProductAdapter adapter;

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
