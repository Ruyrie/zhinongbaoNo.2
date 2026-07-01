package com.example.zhinongbao.mvp.productsearch;

import android.content.Context;

import com.example.zhinongbao.repository.ProductRepository;
import com.example.zhinongbao.repository.PurchaseRepository;

/**
 * ============================================================
 * 【商品搜索 / Product Search】Presenter（业务逻辑）
 * 整体逻辑：start() 一次性给出全部商品 + 采购需求 + 当前用户与是否登录(卖家模式)；
 *   商品的关键词匹配在 View 端本地完成，店铺搜索走仓库查询。
 * 数据来源：同时用两个仓库 —— ProductRepository（商品/店铺）与
 *   PurchaseRepository（采购需求），均经 ContentProvider 访问 SQLite。
 * 配合的文件：接口 = ProductSearchContract；View = ProductSearchActivity。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「商品搜索」可看本组相关文件。
 * ============================================================
 */
public class ProductSearchPresenter implements ProductSearchContract.Presenter {
    private final ProductSearchContract.View view;
    private final ProductRepository productRepository;   // 商品与店铺数据
    private final PurchaseRepository purchaseRepository;  // 采购需求数据

    public ProductSearchPresenter(Context context, ProductSearchContract.View view) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.productRepository = new ProductRepository(appContext);
        this.purchaseRepository = new PurchaseRepository(appContext);
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        String currentUser = purchaseRepository.getLoggedUser();
        view.showInitialData(productRepository.getProducts(), purchaseRepository.getPurchaseRequests(),
                currentUser, currentUser != null && !currentUser.isEmpty());
    }

    @Override
    public void searchStores(String query) {
        view.showStoreResults(productRepository.searchStores(query));
    }

    @Override
    public java.util.List<com.example.zhinongbao.model.PurchaseRequest> getPurchaseRequests() {
        return purchaseRepository.getPurchaseRequests();
    }
}
