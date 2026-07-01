package com.example.zhinongbao.mvp.sellermyproducts;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

/**
 * ============================================================
 * 【我的货品 / Seller My Products】Contract（接口约定）
 * 约定内容：
 *   - View：展示卖家名下的全部货品列表（含已下架）。
 *   - Presenter：刷新列表、删除货品、按商品统计累计订单数与累计销售额。
 * 数据来源：Presenter 走 ProductRepository，内部经 ContentProvider 访问 SQLite；本接口不碰数据库。
 * 配合的文件：View 实现 = activity/SellerMyProductsActivity；Presenter 实现 = SellerMyProductsPresenter；
 *   适配器 = adapter/MyProductAdapter；模型 = model/Product。
 * 在 MVP 数据流中的位置：View 与 Presenter 之间的契约层。
 * 提示：在 IDE 里搜索「我的货品」可看本组相关文件。
 * ============================================================
 */
public interface SellerMyProductsContract {
    interface View extends BaseView<Presenter> {
        void showProducts(List<Product> products); // 展示货品列表
    }

    interface Presenter extends BasePresenter {
        void refresh();                                   // 重新读取并刷新列表
        void deleteProduct(Product product);              // 删除某件货品
        int getProductOrderCount(int productId);          // 某商品累计订单数
        double getProductSalesRevenue(int productId);     // 某商品累计销售额
    }
}
