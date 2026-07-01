package com.example.zhinongbao.mvp.publish;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【发布 / Publish】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来跳转（打开发产品页 / 打开采购市场页）。
 *   - Presenter：由 View 调用来响应两个入口的点击。
 * 数据流向：本页只做导航，不访问 Repository（View 点击 → Presenter → 回调 View 跳转）。
 * 配合的文件：View 实现 = PublishFragment；Presenter 实现 = PublishPresenter。
 * 提示：在 IDE 里搜索「发布」可看本组相关文件。
 * ============================================================
 */
public interface PublishContract {
    // View：Presenter 用这些方法触发页面跳转
    interface View extends BaseView<Presenter> {
        void openAddProduct();     // 打开「发布产品」页
        void openPurchaseMarket(); // 打开「采购市场」页
    }

    // Presenter：View（用户点击入口）用这些方法触发处理
    interface Presenter extends BasePresenter {
        void onPublishProductClicked(); // 点击「发布产品」
        void onPostPurchaseClicked();   // 点击「发布采购需求」
    }
}
