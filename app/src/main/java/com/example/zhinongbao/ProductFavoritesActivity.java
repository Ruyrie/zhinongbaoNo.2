package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.zhinongbao.adapter.ProductAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Product;
import java.util.ArrayList;
import java.util.List;

public class ProductFavoritesActivity extends AppCompatActivity {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataManager dm = DataManager.getInstance(this);
        products.clear();
        products.addAll(dm.getFavoriteProducts(dm.getLoggedUser()));
        if (adapter != null) adapter.notifyDataSetChanged();
        TextView empty = findViewById(R.id.tvProductFavEmpty);
        View rv = findViewById(R.id.rvProductFavorites);
        boolean isEmpty = products.isEmpty();
        rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
