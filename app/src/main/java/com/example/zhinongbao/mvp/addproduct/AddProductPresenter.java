package com.example.zhinongbao.mvp.addproduct;

import android.content.Context;

import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.ProductRepository;
import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【发布/编辑商品 / Add Product】Presenter（业务逻辑）
 * 整体逻辑：start() 回填当前卖家的店铺电话；loadProduct() 载入待编辑商品；
 *   submitProduct() 先更新店铺信息，再按 productId 决定「更新已有商品」或
 *   「新增商品」；买家首次发布商品会自动升级为「买家+卖家」双身份。
 * 数据来源：走 ProductRepository（商品）与 UserRepository（店铺/身份），
 *   内部均经 ContentProvider 访问 SQLite。
 * 配合的文件：接口 = AddProductContract；View = AddProductActivity；模型 = model/Product、model/User。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「发布商品」可看本组相关文件。
 * ============================================================
 */
public class AddProductPresenter implements AddProductContract.Presenter {
    private final AddProductContract.View view;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final String currentUser;

    public AddProductPresenter(Context context, AddProductContract.View view) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.productRepository = new ProductRepository(appContext);
        this.userRepository = new UserRepository(appContext);
        this.currentUser = userRepository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        if (currentUser != null) {
            view.showStorePhone(productRepository.getStorePhone(currentUser));
        }
    }

    @Override
    public void loadProduct(int productId) {
        Product product = productRepository.getProductById(productId);
        if (product != null) {
            view.showExistingProduct(product);
        }
    }

    @Override
    public void submitProduct(int productId, String name, String desc, double price, String coverUri, String categories,
            String storePhone, String brand, String origin, String spec, String packageType) {
        if (currentUser != null) {
            userRepository.updateStoreInfo(currentUser, userRepository.getStoreName(currentUser), storePhone);
        }
        if (productId > 0) {
            productRepository.updateProduct(productId, name, desc, price, coverUri, categories,
                    brand, origin, spec, packageType);
            view.showToast("货品信息已更新", false);
            view.closePage();
            return;
        }
        productRepository.addProduct(name, desc, price, coverUri == null ? "" : coverUri, categories,
                brand, origin, spec, packageType);
        if (currentUser != null && userRepository.getUserRole(currentUser) == User.ROLE_BUYER) {
            userRepository.updateUserRole(currentUser, User.ROLE_BOTH);
            view.showToast("商品发布成功！您已获得卖家身份，下次登录可选择身份", true);
        } else {
            view.showToast("商品发布成功", false);
        }
        view.closePage();
    }
}
