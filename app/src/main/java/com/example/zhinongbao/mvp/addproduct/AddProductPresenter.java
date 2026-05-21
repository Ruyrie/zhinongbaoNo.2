package com.example.zhinongbao.mvp.addproduct;

import android.content.Context;

import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.ProductRepository;
import com.example.zhinongbao.repository.UserRepository;

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
            String storePhone) {
        if (currentUser != null) {
            userRepository.updateStoreInfo(currentUser, userRepository.getStoreName(currentUser), storePhone);
        }
        if (productId > 0) {
            productRepository.updateProduct(productId, name, desc, price, coverUri, categories);
            view.showToast("货品信息已更新", false);
            view.closePage();
            return;
        }
        productRepository.addProduct(name, desc, price, coverUri == null ? "" : coverUri, categories);
        if (currentUser != null && userRepository.getUserRole(currentUser) == User.ROLE_BUYER) {
            userRepository.updateUserRole(currentUser, User.ROLE_BOTH);
            view.showToast("商品发布成功！您已获得卖家身份，下次登录可选择身份", true);
        } else {
            view.showToast("商品发布成功", false);
        }
        view.closePage();
    }
}
