package com.example.zhinongbao.mvp.productdetail;

import android.content.Context;

import com.example.zhinongbao.activity.ChatActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.ProductComment;
import com.example.zhinongbao.repository.ProductRepository;

import java.util.List;

/**
 * ============================================================
 * 【商品详情 / Product Detail】Presenter（业务逻辑）
 * 整体逻辑：start() 取商品(不存在则关页面)、记录一次浏览(足迹)、算出卖家与
 *   是否本人商品，回调 showProduct + 评论预览；buyNow() 会先检查是否有默认
 *   收货地址，没有则引导去添加。
 * 数据来源：走 ProductRepository（内部经 ContentProvider 访问 SQLite）。
 * 关键概念：无明确卖家时用默认店铺账号(ChatActivity.SHOP_USERNAME)兜底。
 * 配合的文件：接口 = ProductDetailContract；View = ProductDetailActivity；
 *   模型 = model/Product、model/ProductComment；联系卖家跳 ChatActivity。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「商品详情」可看本组相关文件。
 * ============================================================
 */
public class ProductDetailPresenter implements ProductDetailContract.Presenter {
    private final ProductDetailContract.View view;   // 回调界面
    private final ProductRepository repository;       // 商品数据访问入口
    private final int productId;                      // 当前商品 id
    private Product product;                          // 缓存商品对象
    private String username;                           // 当前登录用户
    private String seller;                             // 卖家账号（空则用默认店铺兜底）

    public ProductDetailPresenter(Context context, ProductDetailContract.View view, int productId) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.productId = productId;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        product = repository.getProductById(productId);
        if (product == null) {
            view.closePage();
            return;
        }
        username = repository.getLoggedUser();
        repository.recordProductView(username, product.id);
        product = repository.getProductById(productId);
        seller = product.seller != null && !product.seller.isEmpty() ? product.seller : ChatActivity.SHOP_USERNAME;
        boolean ownProduct = username != null && username.equals(seller);
        view.showProduct(product, username, seller, ownProduct, repository.isProductFavorited(username, product.id));
        refreshComments();
    }

    @Override
    public void refreshComments() {
        int count = repository.getProductCommentCount(productId);
        List<ProductComment> comments = repository.getProductComments(productId);
        view.showCommentPreview(count, comments.isEmpty() ? null : comments.get(0));
    }

    @Override
    public void addToCart() {
        if (username == null || username.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (product.isOffShelf()) {
            view.showToast("商品已下架");
            return;
        }
        repository.addToCart(username, product);
        view.showToast("已加入购物车");
    }

    @Override
    public void buyNow() {
        if (product.isOffShelf()) {
            view.showToast("商品已下架");
            return;
        }
        if (repository.getDefaultAddress(username) == null) {
            view.showToast("请先添加收货地址");
            view.openAddressManager();
            return;
        }
        repository.addOrder(username, product, 1);
        view.showToast("下单成功");
        view.openMyOrders();
    }

    @Override
    public void toggleFavorite() {
        if (username == null || username.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        boolean favorited = repository.toggleProductFavorite(username, product.id);
        view.setFavoriteState(favorited);
        view.showToast(favorited ? "已收藏商品" : "已取消收藏");
    }

    @Override
    public void delistProduct() {
        repository.delistProduct(product.id);
        view.showToast("已下架");
        view.closePage();
    }

    @Override
    public void relistProduct() {
        repository.relistProduct(product.id);
        view.showToast("已重新上架");
        view.closePage();
    }

    @Override
    public String getStorePhone() {
        return repository.getStorePhone(seller);
    }
}
