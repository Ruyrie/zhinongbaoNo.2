package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.MyProductAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Product;
import java.util.ArrayList;
import java.util.List;

public class SellerMyProductsActivity extends AppCompatActivity {

    private RecyclerView rvProducts;
    private MyProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private DataManager dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_my_products);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        dm = DataManager.getInstance(this);

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
                            dm.deleteProduct(product.id);
                            productList.remove(position);
                            adapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        rvProducts.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        String user = dm.getLoggedUser();
        if (user != null) {
            productList.clear();
            productList.addAll(dm.getProductsBySeller(user));
            adapter.notifyDataSetChanged();
        }
    }
}