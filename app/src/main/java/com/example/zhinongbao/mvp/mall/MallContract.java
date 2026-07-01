package com.example.zhinongbao.mvp.mall;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

import java.util.List;

/**
 * ============================================================
 * 【商城首页 / Mall】Contract（接口约定）
 * 约定内容：View（展示商品列表、弹提示）；Presenter（刷新商品、加购物车）。
 * 配合的文件：View 实现 = MallFragment；Presenter 实现 = MallPresenter；
 *   数据访问 = repository/ProductRepository；模型 = model/Product。
 * 提示：在 IDE 里搜索「商城」可看本组相关文件。
 * ============================================================
 */
public interface MallContract {
    interface View extends BaseView<Presenter> {
        void showProducts(List<Product> products); // 显示/刷新商品列表
        void showToast(String message);            // 弹提示
    }

    interface Presenter extends BasePresenter {
        void refresh();                 // 重新拉取全部商品
        void addToCart(Product product);// 加入购物车
    }
}
