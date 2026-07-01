package com.example.zhinongbao.mvp.cart;

/* ============================================================
 * 【购物车 / Cart】合同接口（Contract，MVP 模式的「约定书」）
 * ============================================================
 *
 * 为什么要有它：
 *   - View 接口：列出「界面必须提供哪些能力」——比如显示购物车、弹提示。
 *     由 CartActivity 去真正实现这些方法。
 *   - Presenter 接口：列出「业务大脑必须提供哪些能力」——比如算钱、结算。
 *     由 CartPresenter 去真正实现。
 *   这样 Activity 和 Presenter 只认「接口」不认对方具体类，耦合低、好测试、好替换。
 *
 * 配套文件：CartActivity（实现 View）、CartPresenter（实现 Presenter）。
 * 提示：在 IDE 里搜索「购物车」可看本组全部文件。
 * ============================================================ */

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.CartItem;

import java.util.List;

public interface CartContract {
    // View = 界面层要实现的方法。Presenter 通过调用这些方法来「指挥界面更新」。
    interface View extends BaseView<Presenter> {
        void showCart(List<CartItem> items);                 // 把购物车商品列表显示出来

        void updateSummary(double total, int itemCount, boolean allChecked); // 刷新底部合计/件数/全选

        void showToast(String message);                      // 弹一条短提示

        void openAddressManager();                           // 跳转到收货地址管理页

        void openMyOrders();                                 // 跳转到我的订单页

        void closePage();                                    // 关闭当前页面
    }

    // Presenter = 业务层要实现的方法。界面在用户操作后调用这些方法，把活儿交给业务层。
    interface Presenter extends BasePresenter {
        void onCartSelectionChanged(double total, int itemCount, boolean allChecked); // 勾选变化→重算底部栏

        void onCartItemsChanged(List<CartItem> currentItems); // 商品增减/改数量→保存到数据库

        void onClearCart(List<CartItem> currentItems);        // 清空购物车

        void checkout(List<CartItem> currentItems, List<CartItem> checkedItems); // 结算下单
    }
}
