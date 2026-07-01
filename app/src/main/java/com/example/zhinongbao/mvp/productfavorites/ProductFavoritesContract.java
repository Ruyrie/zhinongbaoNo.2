package com.example.zhinongbao.mvp.productfavorites;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

/**
 * ============================================================
 * 【商品收藏 / Product Favorites】Contract（接口约定）
 * 约定内容：
 *   - View：渲染收藏商品列表。
 *   - Presenter：刷新（重新拉取收藏商品）。
 * 配合的文件：View 实现 = ProductFavoritesActivity；Presenter 实现 =
 *   ProductFavoritesPresenter；数据访问 = repository/ProductRepository；
 *   模型 = model/Product。
 * 在 MVP 数据流中的位置：接口层。
 * 提示：在 IDE 里搜索「商品收藏」可看本组相关文件。
 * ============================================================
 */
public interface ProductFavoritesContract {
    // View：Presenter 用这个方法更新界面
    interface View extends BaseView<Presenter> {
        void showProducts(List<Product> products); // 渲染收藏商品列表
    }

    // Presenter：View 用这个方法触发业务
    interface Presenter extends BasePresenter {
        void refresh(); // 重新拉取收藏商品
    }
}
