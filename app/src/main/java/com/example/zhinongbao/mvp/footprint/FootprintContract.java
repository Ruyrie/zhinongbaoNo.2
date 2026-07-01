package com.example.zhinongbao.mvp.footprint;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.StoreFootprint;

import java.util.List;

/**
 * ============================================================
 * 【我的足迹 / Footprint】Contract（接口约定）
 * 约定内容：
 *   - View：显示商品浏览记录、显示店铺浏览记录。
 *   - Presenter：加载商品足迹、加载店铺足迹。
 * 配合的文件：View 实现 = FootprintActivity；Presenter 实现 = FootprintPresenter；
 *   数据访问 = repository/ProductRepository；模型 = model/Product、model/StoreFootprint。
 * 在 MVP 数据流中的位置：接口层，连接 View 与 Presenter。
 * 提示：在 IDE 里搜索「我的足迹」可看本组相关文件。
 * ============================================================
 */
public interface FootprintContract {
    interface View extends BaseView<Presenter> {
        void showProductFootprints(List<Product> products);     // 显示浏览过的商品
        void showStoreFootprints(List<StoreFootprint> stores);  // 显示浏览过的店铺
    }

    interface Presenter extends BasePresenter {
        void loadProducts();    // 加载商品足迹
        void loadStores();      // 加载店铺足迹
    }
}
