package com.example.zhinongbao.mvp.main;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【主页面 / Main】Contract（接口约定）
 * 约定内容：
 *   - View：无额外方法（主界面的 UI 逻辑主要在 Activity 内部完成）。
 *   - Presenter：getActiveRole 取当前角色、isSellerMode 判断是否卖家版本。
 * 配合的文件：View 实现 = MainActivity；Presenter 实现 = MainPresenter；
 *   数据访问 = repository/UserRepository；模型 = model/User。
 * 在 MVP 数据流中的位置：接口层，连接 View 与 Presenter。
 * 提示：在 IDE 里搜索「主页面」可看本组相关文件。
 * ============================================================
 */
public interface MainContract {
    interface View extends BaseView<Presenter> {
    }

    interface Presenter extends BasePresenter {
        int getActiveRole();        // 取当前激活角色（买家/卖家）
        boolean isSellerMode();     // 当前是否为卖家版本
    }
}
