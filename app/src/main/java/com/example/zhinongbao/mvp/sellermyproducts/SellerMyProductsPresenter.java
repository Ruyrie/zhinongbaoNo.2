package com.example.zhinongbao.mvp.sellermyproducts;

import android.content.Context;

import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.repository.ProductRepository;

/**
 * ============================================================
 * 【我的货品 / Seller My Products】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1. 构造时创建 ProductRepository 并取当前登录卖家用户名。
 *   2. start()/refresh()：取该卖家名下全部货品（含已下架）回调 View 展示。
 *   3. deleteProduct()：删除后重新 refresh。
 *   4. 统计方法：按商品 id 取累计订单数与累计销售额，供适配器展示。
 * 数据来源：ProductRepository，内部经 ContentProvider 访问 SQLite；本类不直接碰数据库。
 * 配合的文件：接口 = SellerMyProductsContract；View = activity/SellerMyProductsActivity；模型 = model/Product。
 * 在 MVP 数据流中的位置：View 与 Repository 之间的业务中枢。
 * 提示：在 IDE 里搜索「我的货品」可看本组相关文件。
 * ============================================================
 */
public class SellerMyProductsPresenter implements SellerMyProductsContract.Presenter {
    private final SellerMyProductsContract.View view;
    private final ProductRepository repository;
    private final String currentUser;

    public SellerMyProductsPresenter(Context context, SellerMyProductsContract.View view) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        // 「我的货品」展示全部（含已下架），方便卖家管理与删除
        view.showProducts(repository.getAllProductsBySeller(currentUser));
    }

    @Override
    public void deleteProduct(Product product) {
        repository.deleteProduct(product.id);
        refresh();
    }

    @Override
    public int getProductOrderCount(int productId) {
        return repository.getProductOrderCount(productId);
    }

    @Override
    public double getProductSalesRevenue(int productId) {
        return repository.getProductSalesRevenue(productId);
    }
}
