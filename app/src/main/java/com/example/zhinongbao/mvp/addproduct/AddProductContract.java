package com.example.zhinongbao.mvp.addproduct;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;

/**
 * ============================================================
 * 【发布/编辑商品 / Add Product】Contract（接口约定）
 * 约定内容：View（显示店铺电话、回填已有商品用于编辑、弹提示、关页面）；
 *   Presenter（loadProduct 载入待编辑商品、submitProduct 新增或更新商品）。
 * 关键概念：同一页面既用于「新增商品」也用于「编辑商品」，靠 productId 是否 >0 区分。
 * 配合的文件：View 实现 = AddProductActivity；Presenter 实现 = AddProductPresenter；
 *   数据访问 = repository/ProductRepository、repository/UserRepository；模型 = model/Product。
 * 提示：在 IDE 里搜索「发布商品」可看本组相关文件。
 * ============================================================
 */
public interface AddProductContract {
    interface View extends BaseView<Presenter> {
        void showStorePhone(String phone);
        void showExistingProduct(Product product);
        void showToast(String message, boolean longToast);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void loadProduct(int productId);
        void submitProduct(int productId, String name, String desc, double price, String coverUri, String categories,
                String storePhone, String brand, String origin, String spec, String packageType);
    }
}
