package com.example.zhinongbao.mvp.sellerstore;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

/**
 * ============================================================
 * 【卖家店铺 / Seller Store】Contract（接口约定）
 * 约定内容：
 *   - View：展示店铺资料、商品列表、销售统计、关注状态；弹提示。
 *   - Presenter：加载店铺、关注/取关、刷新商品与统计、上下架商品、修改店铺信息。
 * 关键概念：同一页面既是「我的店铺」(可编辑、可上下架、看统计)也可作为别人店铺公开浏览(只看在售)，
 *   由 ownStore 与 public_store 参数共同决定。
 * 数据来源：Presenter 走 ProductRepository/UserRepository/OrderRepository/ArticleRepository，
 *   内部经 ContentProvider 访问 SQLite；本接口不碰数据库。
 * 配合的文件：View 实现 = activity/SellerStoreActivity（内含店铺商品适配器）；
 *   Presenter 实现 = SellerStorePresenter；模型 = model/Product。
 * 在 MVP 数据流中的位置：View 与 Presenter 之间的契约层。
 * 提示：在 IDE 里搜索「卖家店铺」可看本组相关文件。
 * ============================================================
 */
public interface SellerStoreContract {
    interface View extends BaseView<Presenter> {
        void showStoreMeta(String seller, String storeName, String storePhone, String avatarUri, boolean ownStore); // 展示店铺头部资料
        void showProducts(List<Product> products);       // 展示店铺商品列表
        void showStats(int totalOrders, double totalRevenue); // 展示销售统计（自己店铺才有）
        // following=当前是否已关注；canFollow=是否显示关注按钮（非自己店铺且已登录）
        void showFollowState(boolean following, boolean canFollow);
        void showToast(String message);                  // 弹提示
    }

    interface Presenter extends BasePresenter {
        String getCurrentUser();                 // 取当前登录用户名
        void loadStore(String seller);           // 加载指定卖家的店铺（空则为自己）
        void toggleFollow();                     // 关注 / 取消关注
        void refreshProducts();                  // 刷新商品列表
        void refreshStats();                     // 刷新销售统计
        int getProductOrderCount(int productId); // 某商品已售数量
        void delistProduct(int productId);       // 下架商品
        void relistProduct(int productId);       // 重新上架商品
        void updateStoreInfo(String name, String phone); // 修改店铺名称与电话
    }
}
