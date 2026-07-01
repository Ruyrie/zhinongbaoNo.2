package com.example.zhinongbao.mvp.sellerstore;

import android.content.Context;

import com.example.zhinongbao.repository.ArticleRepository;
import com.example.zhinongbao.repository.OrderRepository;
import com.example.zhinongbao.repository.ProductRepository;
import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【卖家店铺 / Seller Store】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1. 构造时创建四个 Repository 并取当前登录用户名。
 *   2. loadStore()：确定要看的卖家，记录一次浏览，判断是否本人店铺(ownStore)，
 *      回调店铺资料、商品列表、（本人才有的）统计、关注状态。
 *   3. refreshProducts()：本人店铺显示全部(含已下架)，看别人只显示在售。
 *   4. toggleFollow / delist / relist / updateStoreInfo 处理关注、上下架、改资料等操作并刷新。
 * 数据来源：ProductRepository/UserRepository/OrderRepository/ArticleRepository，
 *   内部经 ContentProvider 访问 SQLite；本类不直接碰数据库。
 * 配合的文件：接口 = SellerStoreContract；View = activity/SellerStoreActivity；模型 = model/Product。
 * 在 MVP 数据流中的位置：View 与 Repository 之间的业务中枢。
 * 提示：在 IDE 里搜索「卖家店铺」可看本组相关文件。
 * ============================================================
 */
public class SellerStorePresenter implements SellerStoreContract.Presenter {
    private final SellerStoreContract.View view;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ArticleRepository articleRepository;
    private final String currentUser;
    private String seller;
    private boolean ownStore;

    public SellerStorePresenter(Context context, SellerStoreContract.View view) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.productRepository = new ProductRepository(appContext);
        this.userRepository = new UserRepository(appContext);
        this.orderRepository = new OrderRepository(appContext);
        this.articleRepository = new ArticleRepository(appContext);
        this.currentUser = userRepository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public String getCurrentUser() {
        return currentUser;
    }

    @Override
    public void loadStore(String seller) {
        this.seller = seller == null || seller.isEmpty() ? currentUser : seller;
        productRepository.recordStoreView(currentUser, this.seller);
        this.ownStore = this.seller != null && this.seller.equals(currentUser);
        view.showStoreMeta(this.seller, ownStore ? "我的店铺" : userRepository.getStoreName(this.seller),
                productRepository.getStorePhone(this.seller), userRepository.getAvatarUri(this.seller), ownStore);
        refreshProducts();
        if (ownStore) {
            refreshStats();
        }
        refreshFollowState();
    }

    private void refreshFollowState() {
        // 关注按钮：仅在「已登录 且 不是自己的店铺」时显示
        boolean canFollow = currentUser != null && !currentUser.isEmpty() && !ownStore
                && seller != null && !seller.isEmpty();
        boolean following = canFollow && articleRepository.isFollowingStore(currentUser, seller);
        view.showFollowState(following, canFollow);
    }

    @Override
    public void toggleFollow() {
        if (currentUser == null || currentUser.isEmpty() || ownStore || seller == null || seller.isEmpty()) {
            return;
        }
        if (articleRepository.isFollowingStore(currentUser, seller)) {
            articleRepository.unfollowStore(currentUser, seller);
            view.showToast("已取消关注");
        } else {
            articleRepository.followStore(currentUser, seller);
            view.showToast("已关注");
        }
        refreshFollowState();
    }

    @Override
    public void refreshProducts() {
        // 自己的店铺展示全部（含已下架，便于重新上架）；看别人店铺只展示在售
        view.showProducts(ownStore
                ? productRepository.getAllProductsBySeller(seller)
                : productRepository.getProductsBySeller(seller));
    }

    @Override
    public void refreshStats() {
        view.showStats(orderRepository.getTotalOrderCountForSeller(seller), orderRepository.getTotalRevenueForSeller(seller));
    }

    @Override
    public int getProductOrderCount(int productId) {
        return productRepository.getProductOrderCount(productId);
    }

    @Override
    public void delistProduct(int productId) {
        productRepository.delistProduct(productId);
        refreshProducts();
        refreshStats();
    }

    @Override
    public void relistProduct(int productId) {
        productRepository.relistProduct(productId);
        refreshProducts();
        refreshStats();
    }

    @Override
    public void updateStoreInfo(String name, String phone) {
        userRepository.updateStoreInfo(seller, name, phone);
        loadStore(seller);
        view.showToast("店铺信息已更新");
    }
}
