package com.example.zhinongbao.mvp.footprint;

import android.content.Context;

import com.example.zhinongbao.repository.ProductRepository;

/**
 * ============================================================
 * 【我的足迹 / Footprint】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：构造时创建 ProductRepository 并注入 View；
 *   start 默认加载商品足迹；loadProducts / loadStores 分别取对应记录回调 View。
 * 数据来源：走 repository/ProductRepository；Repository 内部经 ContentProvider
 *   访问 SQLite，本类不直接碰数据库。
 * 配合的文件：接口约定 = FootprintContract；View = FootprintActivity；
 *   模型 = model/Product、model/StoreFootprint。
 * 在 MVP 数据流中的位置：Presenter（业务层），承上（View）启下（Repository）。
 * 提示：在 IDE 里搜索「我的足迹」可看本组相关文件。
 * ============================================================
 */
public class FootprintPresenter implements FootprintContract.Presenter {
    private final FootprintContract.View view;      // 关联的界面
    private final ProductRepository repository;     // 商品/足迹数据访问入口

    public FootprintPresenter(Context context, FootprintContract.View view) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        loadProducts();
    }

    @Override
    public void loadProducts() {
        view.showProductFootprints(repository.getProductFootprints(repository.getLoggedUser()));
    }

    @Override
    public void loadStores() {
        view.showStoreFootprints(repository.getStoreFootprints(repository.getLoggedUser()));
    }
}
