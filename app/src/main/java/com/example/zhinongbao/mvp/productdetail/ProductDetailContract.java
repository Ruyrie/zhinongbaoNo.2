package com.example.zhinongbao.mvp.productdetail;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.ProductComment;

/**
 * ============================================================
 * 【商品详情 / Product Detail】Contract（接口约定）
 * 约定内容：
 *   - View：展示商品信息、评论预览、收藏状态；弹提示；跳地址管理/我的订单；关页面。
 *   - Presenter：刷新评论、加购、立即购买、收藏切换、下架/重新上架、取店铺电话。
 * 关键概念：同一页面对「买家」和「商品主人(卖家本人)」表现不同 ——
 *   ownProduct=true 时显示下架/上架，否则显示加购/购买/收藏。
 * 配合的文件：View 实现 = ProductDetailActivity；Presenter 实现 = ProductDetailPresenter；
 *   数据访问 = repository/ProductRepository；模型 = model/Product、model/ProductComment。
 * 提示：在 IDE 里搜索「商品详情」可看本组相关文件。
 * ============================================================
 */
public interface ProductDetailContract {
    interface View extends BaseView<Presenter> {
        // 展示商品（含当前用户、卖家、是否本人商品、是否已收藏）
        void showProduct(Product product, String username, String seller, boolean ownProduct, boolean favorited);
        void showCommentPreview(int count, ProductComment latest); // 评论数 + 最新一条预览
        void showToast(String message);          // 弹提示
        void setFavoriteState(boolean favorited);// 更新收藏按钮状态
        void openAddressManager();               // 跳收货地址管理（无地址时）
        void openMyOrders();                     // 下单成功后跳我的订单
        void closePage();                        // 关闭页面（商品不存在/已下架）
    }

    interface Presenter extends BasePresenter {
        void refreshComments();   // 刷新评论预览
        void addToCart();         // 加入购物车
        void buyNow();            // 立即购买（下单）
        void toggleFavorite();    // 收藏 / 取消收藏
        void delistProduct();     // 下架（卖家本人）
        void relistProduct();     // 重新上架（卖家本人）
        String getStorePhone();   // 取卖家/店铺联系电话
    }
}
