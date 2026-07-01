package com.example.zhinongbao.mvp.productsearch;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.model.StoreSearchResult;

import java.util.List;

/**
 * ============================================================
 * 【商品搜索 / Product Search】Contract（接口约定）
 * 约定内容：View（给出初始数据：商品+采购需求+是否卖家模式；展示店铺搜索结果）；
 *   Presenter（按关键词搜店铺、取采购需求列表）。
 * 配合的文件：View 实现 = ProductSearchActivity；Presenter 实现 = ProductSearchPresenter；
 *   数据访问 = repository/ProductRepository、repository/PurchaseRepository；
 *   模型 = model/Product、model/PurchaseRequest、model/StoreSearchResult。
 * 提示：在 IDE 里搜索「商品搜索」可看本组相关文件。
 * ============================================================
 */
public interface ProductSearchContract {
    interface View extends BaseView<Presenter> {
        // 进页面时给出初始数据（商品、采购需求、当前用户、是否卖家模式）
        void showInitialData(List<Product> products, List<PurchaseRequest> purchaseRequests, String currentUser,
                boolean sellerMode);
        void showStoreResults(List<StoreSearchResult> stores); // 店铺搜索结果
    }

    interface Presenter extends BasePresenter {
        void searchStores(String query);              // 按关键词搜店铺
        List<PurchaseRequest> getPurchaseRequests();  // 取采购需求列表
    }
}
