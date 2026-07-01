package com.example.zhinongbao.mvp.productcomments;

import android.content.Context;

import com.example.zhinongbao.model.ProductComment;
import com.example.zhinongbao.repository.ProductRepository;

/**
 * ============================================================
 * 【商品评价列表 / Product Comments】Presenter（业务逻辑）
 * 整体逻辑：refresh 从 Repository 取该商品评价并连同当前用户名回调 View；
 *   writeReview 时先判断当前用户是否购买过该商品（或为 admin），有资格才跳写评价页，
 *   否则提示需购买；deleteComment 调 Repository 删除后刷新。
 * 数据来源：走 repository/ProductRepository，Repository 内部通过 ContentProvider
 *   访问 SQLite；本类不直接操作数据库。
 * 配合的文件：接口约定 mvp/productcomments/ProductCommentsContract；View 实现 =
 *   ProductCommentsActivity；模型 = model/ProductComment。
 * 在 MVP 数据流中的位置：业务层。
 * 提示：在 IDE 里搜索「商品评价」可看本组相关文件。
 * ============================================================
 */
public class ProductCommentsPresenter implements ProductCommentsContract.Presenter {
    private final ProductCommentsContract.View view; // 对应的界面
    private final ProductRepository repository;      // 商品/评价数据访问
    private final int productId;                     // 当前商品 id
    private final String currentUser;                // 当前登录用户（用于判断可删/购买资格）

    public ProductCommentsPresenter(Context context, ProductCommentsContract.View view, int productId) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.productId = productId;
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showComments(repository.getProductComments(productId), currentUser);
    }

    @Override
    public void writeReview() {
        if (repository.hasPurchasedProduct(currentUser, productId) || "admin".equals(currentUser)) {
            view.openAddComment(productId);
        } else {
            view.showToast("购买该商品后才可以进行评价哦");
        }
    }

    @Override
    public void deleteComment(ProductComment comment) {
        repository.deleteProductComment(comment.id);
        refresh();
    }
}
