package com.example.zhinongbao.mvp.addproductcomment;

import android.content.Context;

import com.example.zhinongbao.repository.ProductRepository;

/**
 * ============================================================
 * 【发表商品评价 / Add Product Comment】Presenter（业务逻辑）
 * 整体逻辑：canComment 判断当前用户是否购买过该商品（或为 admin）；
 *   getReviewBlockMessage 区分「有订单但未确认收货」与「没买过」给出不同提示；
 *   submit 时校验文字与图片不能同时为空，通过 Repository 写入评价后提示并关闭页面。
 * 数据来源：走 repository/ProductRepository，Repository 内部通过 ContentProvider
 *   访问 SQLite；本类不直接操作数据库。
 * 配合的文件：接口约定 mvp/addproductcomment/AddProductCommentContract；View 实现 =
 *   AddProductCommentActivity。
 * 在 MVP 数据流中的位置：业务层。
 * 提示：在 IDE 里搜索「发表评价」可看本组相关文件。
 * ============================================================
 */
public class AddProductCommentPresenter implements AddProductCommentContract.Presenter {
    private final AddProductCommentContract.View view; // 对应的界面
    private final ProductRepository repository;        // 商品/评价数据访问
    private final int productId;                       // 要评价的商品 id
    private final String username;                     // 当前登录用户

    public AddProductCommentPresenter(Context context, AddProductCommentContract.View view, int productId) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.productId = productId;
        this.username = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public boolean canComment() {
        return repository.hasPurchasedProduct(username, productId) || "admin".equals(username);
    }

    @Override
    public String getReviewBlockMessage() {
        if (repository.hasUnconfirmedOrder(username, productId)) {
            return "请确认收货后再评价~";
        }
        return "购买并确认收货后才能评价哦~";
    }

    @Override
    public void submit(String content, String images) {
        if ((content == null || content.isEmpty()) && (images == null || images.isEmpty())) {
            view.showToast("评价内容和图片不能同时为空");
            return;
        }
        repository.addProductComment(productId, username, content, images);
        view.showToast("评价发布成功");
        view.closePage();
    }
}
