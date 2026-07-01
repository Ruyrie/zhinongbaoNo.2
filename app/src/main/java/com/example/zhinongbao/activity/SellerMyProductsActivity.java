package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.MyProductAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.mvp.sellermyproducts.SellerMyProductsContract;
import com.example.zhinongbao.mvp.sellermyproducts.SellerMyProductsPresenter;
import com.example.zhinongbao.utils.DialogUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【我的货品 / Seller My Products】View（Activity 页面）
 * 整体逻辑（关键步骤）：
 *   1. onCreate 加载布局 activity_seller_my_products.xml，配置 RecyclerView 与 MyProductAdapter。
 *   2. 适配器回调 onEdit 跳到 AddProductActivity（带 product_id 进入编辑模式）；
 *      onDelete 弹确认框后调用 presenter.deleteProduct 删除。
 *   3. 适配器通过 ProductStatsDelegate 向 Presenter 取每件商品的累计订单数与销售额。
 *   4. 创建 SellerMyProductsPresenter 并 start()；onResume 时 refresh 刷新列表。
 * 数据来源：不直接碰数据库；数据由 SellerMyProductsPresenter 经 ProductRepository
 *   （内部走 ContentProvider 访问 SQLite）取得后回调 showProducts。
 * 配合的文件：接口 = mvp/sellermyproducts/SellerMyProductsContract；Presenter = SellerMyProductsPresenter；
 *   适配器 = adapter/MyProductAdapter；布局 = res/layout/activity_seller_my_products.xml；
 *   跳转页 = AddProductActivity；模型 = model/Product。
 * 在 MVP 数据流中的位置：View（界面层）。
 * 提示：在 IDE 里搜索「我的货品」可看本组相关文件。
 * ============================================================
 */
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
                DialogUtils.showConfirm(SellerMyProductsActivity.this, "删除货品",
                        "确定要删除货品「" + product.name + "」吗？",
                        "取消", "删除", true, () -> {
                            presenter.deleteProduct(product);
                            return true;
                        });
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
