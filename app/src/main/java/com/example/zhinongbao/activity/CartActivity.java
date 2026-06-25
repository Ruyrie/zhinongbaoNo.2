package com.example.zhinongbao.activity;

/* ============================================================
 * 【购物车 / Cart】购物车页面（View 视图层 · 界面）
 * ============================================================
 * 这个文件是干什么的：
 *   显示购物车里的商品列表，支持「单选 / 全选、加减数量、清空、结算下单」。
 *   它是用户能看到、能点的那一屏。
 *
 * 用到的技术：
 *   - Activity：Android 里「一个界面 / 一屏」就是一个 Activity。
 *   - RecyclerView + CartAdapter：用来高效显示「可以上下滚动的列表」，
 *     每一行长什么样由 item_cart.xml 决定，由 CartAdapter 把数据塞进去。
 *   - MVP 模式：本类只负责「界面显示和点击」，真正的业务逻辑（算钱、下单、
 *     读写数据库）全部交给 CartPresenter 去做，互不干扰、方便维护。
 *
 * 在购物车这组文件里的位置（按数据流向，从上到下）：
 *   CartActivity（本文件，界面）
 *     → CartPresenter（业务逻辑：算钱 / 校验 / 下单）
 *       → CartRepository（读写数据库的工具）
 *         → ZhiNongBaoProvider（全 App 数据库的统一入口）
 *   列表的每一行 = CartAdapter + item_cart.xml 一起渲染
 *   单条商品数据 = CartItem 这个「模型类」装着
 *
 * 提示：在 IDE 里搜索「购物车」即可找到本组所有文件。
 * ============================================================ */

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.CartAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.CartItem;
import com.example.zhinongbao.mvp.cart.CartContract;
import com.example.zhinongbao.mvp.cart.CartPresenter;
import com.example.zhinongbao.utils.DialogUtils;
import java.util.ArrayList;
import java.util.List;

// 继承 BaseMvpActivity（拿到 presenter 字段）；implements CartContract.View 表示
// 「我是购物车的界面，必须实现合同里规定的那些界面方法（如 showCart、updateSummary）」。
public class CartActivity extends BaseMvpActivity<CartContract.Presenter> implements CartContract.View {

    private CartAdapter adapter;                              // 列表适配器：负责把 items 一条条画到屏幕上
    private final List<CartItem> items = new ArrayList<>();   // 购物车里的所有商品（内存中的数据）
    private CheckBox cbSelectAll;                             // 底部「全选」勾选框
    private TextView tvTotal, tvCount;                        // 底部「合计金额」「共几件」两个文字

    // onCreate：界面创建时第一个被系统调用的方法，相当于「页面的入口/初始化」
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);              // 把 activity_cart.xml 这个布局显示出来

        findViewById(R.id.tvBack).setOnClickListener(v -> finish()); // 点左上角返回箭头 → 关闭本页

        // 准备列表：RecyclerView 是「列表容器」，LinearLayoutManager 表示「竖着一行行排列」
        RecyclerView rv = findViewById(R.id.rvCart);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CartAdapter(items);                    // 把内存里的 items 交给适配器管理
        adapter.setOnChangeListener(this::onCartItemsChanged); // 列表里任何改动（加减/勾选）都回调这里
        rv.setAdapter(adapter);                              // 把适配器装到列表上，列表才会显示内容

        // 找到底部栏的各个控件（findViewById = 按 id 从布局里把控件拿出来）
        cbSelectAll = findViewById(R.id.cbSelectAll);
        tvTotal = findViewById(R.id.tvCartTotal);
        tvCount = findViewById(R.id.tvCartCount);
        Button btnCheckout = findViewById(R.id.btnCheckout); // 「结算」按钮
        TextView tvClear = findViewById(R.id.tvClearCart);   // 「清空购物车」文字按钮

        // 勾选/取消「全选」时，让适配器把所有商品一起勾上或取消
        cbSelectAll.setOnCheckedChangeListener((btn, checked) -> adapter.setAllChecked(checked));

        btnCheckout.setOnClickListener(v -> checkout());     // 点「结算」→ 走结算流程

        // 点「清空」：先判断购物车是否为空，不为空则弹确认框，用户确认后才真正清空
        tvClear.setOnClickListener(v -> {
            if (items.isEmpty()) {
                showToast("购物车已经是空的");
                return;
            }
            DialogUtils.showConfirm(this,
                    "清空购物车",
                    "将移除购物车中的全部商品，清空后需要重新添加。",
                    "再想想",
                    "确认清空",
                    true,
                    () -> {
                        adapter.clearAll();                  // 界面上清空
                        presenter.onClearCart(items);        // 通知业务层把数据库里也清空
                        return true;
                    });
        });

        // 创建 Presenter（业务大脑）并调用 start()：start() 会去数据库把购物车数据读出来
        new CartPresenter(this, this).start();
        refreshBottomBar();                                  // 刷新底部「合计/件数/全选」状态
    }

    // 重新计算底部栏：把「选中商品的总价、商品总件数、是否全选」交给 Presenter 处理
    private void refreshBottomBar() {
        double total = adapter.getSelectedTotal();
        presenter.onCartSelectionChanged(total, items.size(), adapter.areAllChecked());
    }

    // 购物车内容发生变化（加减数量/勾选）时：刷新底部栏 + 通知业务层把最新数据存进数据库
    private void onCartItemsChanged() {
        refreshBottomBar();
        presenter.onCartItemsChanged(items);
    }

    // 点结算：把「全部商品」和「勾选的商品」都交给 Presenter，由它去校验并下单
    private void checkout() {
        presenter.checkout(items, adapter.getCheckedItems());
    }

    // ↓↓↓ 下面这些是 CartContract.View 规定的方法，由 Presenter 反过来调用，用于「更新界面」↓↓↓

    // Presenter 把数据库里读到的购物车数据交回来，这里刷新到屏幕上
    @Override
    public void showCart(List<CartItem> newItems) {
        items.clear();
        items.addAll(newItems);
        adapter.notifyDataSetChanged();                      // 告诉列表「数据变了，重新画一遍」
        refreshBottomBar();
    }

    // 更新底部栏文字：合计金额、共几件、全选框是否打勾
    @Override
    public void updateSummary(double total, int itemCount, boolean allChecked) {
        tvTotal.setText(String.format("¥%.2f", total));      // %.2f = 保留两位小数，如 ¥12.50
        tvCount.setText("共 " + itemCount + " 件");
        // 先把监听器设为 null 再设置勾选状态，避免「代码改勾选」误触发监听器造成死循环
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(allChecked);
        cbSelectAll.setOnCheckedChangeListener((btn, checked) -> adapter.setAllChecked(checked));
    }

    // 弹出一条短提示（Toast = 屏幕下方一闪而过的小黑条提示）
    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // 跳转到「收货地址管理」页面（结算时若没有默认地址，会引导用户先去添加）
    @Override
    public void openAddressManager() {
        startActivity(new Intent(this, AddressManagerActivity.class));
    }

    // 跳转到「我的订单」页面（下单成功后跳过去看订单）
    @Override
    public void openMyOrders() {
        startActivity(new Intent(this, MyOrdersActivity.class));
    }

    // 关闭当前页面（finish() = 结束这个 Activity，返回上一页）
    @Override
    public void closePage() {
        finish();
    }
}
