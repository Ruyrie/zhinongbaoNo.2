package com.example.zhinongbao.mvp.productfavorites;

import android.content.Context;

import com.example.zhinongbao.repository.ProductRepository;

/**
 * ============================================================
 * 【商品收藏 / Product Favorites】Presenter（业务逻辑）
 * 整体逻辑：refresh 时从 Repository 取当前用户的收藏商品，回调 View 渲染。
 * 数据来源：走 repository/ProductRepository，Repository 内部通过 ContentProvider
 *   访问 SQLite；本类不直接操作数据库。
 * 配合的文件：接口约定 mvp/productfavorites/ProductFavoritesContract；View 实现 =
 *   ProductFavoritesActivity；模型 = model/Product。
 * 在 MVP 数据流中的位置：业务层。
 * 提示：在 IDE 里搜索「商品收藏」可看本组相关文件。
 * ============================================================
 */
public class ProductFavoritesPresenter implements ProductFavoritesContract.Presenter {
    private final ProductFavoritesContract.View view; // 对应的界面
    private final ProductRepository repository;       // 商品数据访问

    public ProductFavoritesPresenter(Context context, ProductFavoritesContract.View view) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showProducts(repository.getFavoriteProducts(repository.getLoggedUser()));
    }
}
