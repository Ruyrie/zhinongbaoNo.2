package com.example.zhinongbao.mvp.mall;

import android.content.Context;

import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.repository.ProductRepository;

/**
 * ============================================================
 * 【商城首页 / Mall】Presenter（业务逻辑）
 * 整体逻辑：start()/refresh() 从仓库取全部商品交给 View 展示；addToCart()
 *   先拦截「购买自己发布的商品」再加购。分类筛选(推荐/水果蔬菜等)在 View
 *   里本地过滤，不在此处。
 * 数据来源：走 ProductRepository（内部经 ContentProvider 访问 SQLite）。
 * 配合的文件：接口 = MallContract；View = MallFragment；模型 = model/Product。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「商城」可看本组相关文件。
 * ============================================================
 */
public class MallPresenter implements MallContract.Presenter {
    private final MallContract.View view;          // 回调界面
    private final ProductRepository repository;     // 商品数据访问入口
    private final String currentUser;               // 当前登录用户（用于加购与自购拦截）

    public MallPresenter(Context context, MallContract.View view) {
        this.view = view;
        this.repository = new ProductRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showProducts(repository.getProducts());
    }

    @Override
    public void addToCart(Product product) {
        if (product.seller != null && product.seller.equals(currentUser)) {
            view.showToast("不能购买自己发布的商品");
            return;
        }
        repository.addToCart(currentUser, product);
        view.showToast("已加入购物车");
    }
}
