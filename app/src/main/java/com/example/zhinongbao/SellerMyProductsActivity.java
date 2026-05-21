package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.MyProductAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.mvp.sellermyproducts.SellerMyProductsContract;
import com.example.zhinongbao.mvp.sellermyproducts.SellerMyProductsPresenter;
import java.util.ArrayList;
import java.util.List;

public class SellerMyProductsActivity extends BaseMvpActivity<SellerMyProductsContract.Presenter>
        implements SellerMyProductsContract.View {

    private RecyclerView rvProducts;
    private MyProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_my_products);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        rvProducts = findViewById(R.id.rvMyProducts);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MyProductAdapter(productList, new MyProductAdapter.OnProductActionListener() {
            @Override
            public void onEdit(Product product) {
                Intent i = new Intent(SellerMyProductsActivity.this, AddProductActivity.class);
                i.putExtra("product_id", product.id);
                startActivity(i);
            }

            @Override
            public void onDelete(Product product, int position) {
                new AlertDialog.Builder(SellerMyProductsActivity.this)
                        .setTitle("删除货品")
                        .setMessage("确定要删除货品「" + product.name + "」吗？")
                        .setPositiveButton("删除", (d, w) -> {
                            presenter.deleteProduct(product);
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        }, new MyProductAdapter.ProductStatsDelegate() {
            @Override
            public int getProductOrderCount(int productId) {
                return presenter.getProductOrderCount(productId);
            }

            @Override
            public double getProductSalesRevenue(int productId) {
                return presenter.getProductSalesRevenue(productId);
            }
        });
        rvProducts.setAdapter(adapter);
        new SellerMyProductsPresenter(this, this).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.refresh();
        }
    }

    @Override
    public void showProducts(List<Product> products) {
        productList.clear();
        productList.addAll(products);
        adapter.notifyDataSetChanged();
    }
}
