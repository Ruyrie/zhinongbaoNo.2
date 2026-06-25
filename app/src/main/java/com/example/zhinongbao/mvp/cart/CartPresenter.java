package com.example.zhinongbao.mvp.cart;

/* ============================================================
 * 【购物车 / Cart】业务逻辑（Presenter，MVP 模式的「大脑」）
 * ============================================================
 * 这个文件是干什么的：
 *   购物车的所有「思考和判断」都在这里：算总价、保存改动、清空、
 *   结算前的各种校验（有没有选商品、是不是自己的货、有没有收货地址）、生成订单。
 *
 * 关键特点：
 *   - 它不碰任何按钮、文字、图片等界面控件（那是 Activity 的事）。
 *   - 它通过 view（CartContract.View）来「指挥界面」：比如 view.showToast(...)。
 *   - 它通过 repository（CartRepository）来「读写数据库」。
 *   这样「业务」和「界面」彻底分开，逻辑清晰、方便单独测试。
 *
 * 数据流向：CartActivity → 本类(CartPresenter) → CartRepository → 数据库。
 * 提示：在 IDE 里搜索「购物车」可看本组全部文件。
 * ============================================================ */

import android.content.Context;

import com.example.zhinongbao.model.CartItem;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.repository.CartRepository;

import java.util.List;

public class CartPresenter implements CartContract.Presenter {
    private final CartContract.View view;          // 界面（只认接口，不直接认 CartActivity）
    private final CartRepository repository;        // 数据仓库：负责和数据库打交道
    private final String username;                  // 当前登录的用户名（购物车按用户区分）

    // 构造方法：界面创建本 Presenter 时调用。这里把 view、repository、当前用户都准备好。
    public CartPresenter(Context context, CartContract.View view) {
        this.view = view;
        this.repository = new CartRepository(context.getApplicationContext());
        this.username = repository.getLoggedUser();  // 从本地登录信息里取出当前用户名
        this.view.setPresenter(this);                // 把自己交给界面，让界面也能回调本 Presenter
    }

    // start：界面初始化时调用，去数据库读出该用户的购物车，再交给界面显示
    @Override
    public void start() {
        view.showCart(repository.getCart(username));
    }

    // 勾选变化时：本类不做复杂计算（金额已由适配器算好），直接让界面刷新底部栏
    @Override
    public void onCartSelectionChanged(double total, int itemCount, boolean allChecked) {
        view.updateSummary(total, itemCount, allChecked);
    }

    // 商品增减/改数量时：把当前购物车整体保存到数据库
    @Override
    public void onCartItemsChanged(List<CartItem> currentItems) {
        repository.saveCart(username, currentItems);
    }

    // 清空购物车：清掉内存数据 → 保存空列表到数据库 → 让底部栏归零
    @Override
    public void onClearCart(List<CartItem> currentItems) {
        currentItems.clear();
        repository.saveCart(username, currentItems);
        view.updateSummary(0, 0, false);
    }

    // 结算下单：这是购物车最核心的业务，分几步层层校验，全部通过才真正生成订单
    @Override
    public void checkout(List<CartItem> currentItems, List<CartItem> checkedItems) {
        // 校验①：必须至少选中一件商品
        if (checkedItems.isEmpty()) {
            view.showToast("请先选择商品");
            return;
        }
        // 校验②：不能购买自己发布的商品（卖家 == 当前用户则拦截）
        for (CartItem item : checkedItems) {
            Product product = repository.getProductById(item.productId);
            if (product != null && product.seller != null && product.seller.equals(username)) {
                view.showToast("不能结算自己发布的商品");
                return;
            }
        }
        // 校验③：必须有默认收货地址，否则引导用户先去添加地址
        if (repository.getDefaultAddress(username) == null) {
            view.showToast("请先添加收货地址");
            view.openAddressManager();
            return;
        }
        // 全部校验通过：为每件选中的商品生成一条订单
        for (CartItem item : checkedItems) {
            repository.addOrder(username, item);
        }
        // 已下单的商品从购物车移除，并保存最新购物车
        currentItems.removeAll(checkedItems);
        repository.saveCart(username, currentItems);
        // 跳到「我的订单」页，并关闭购物车页
        view.openMyOrders();
        view.closePage();
    }
}
