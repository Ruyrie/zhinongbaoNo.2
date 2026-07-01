package com.example.zhinongbao.mvp.agritech;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【农技学堂 / AgriTech】Contract（接口约定）
 * 约定内容：View 只需实现 loadAgriTechPage(url) —— 收到地址就把网页加载出来；
 *   Presenter 继承 BasePresenter（自带 start()）。农技学堂功能简单，
 *   所以 Presenter 这里没有再加额外方法。
 * 配合的文件：View 实现 = AgriTechFragment；Presenter 实现 = AgriTechPresenter；
 *   基类 = base/BaseView、base/BasePresenter。
 * 提示：在 IDE 里搜索「农技学堂」可看本组相关文件。
 * ============================================================
 */
public interface AgriTechContract {
    // View 层需要实现的方法（给 Presenter 调用去更新界面）
    interface View extends BaseView<Presenter> {
        void loadAgriTechPage(String url); // 加载指定地址的网页
    }

    // Presenter 层需要实现的方法（供 View 调用触发业务）；此功能仅需基类的 start()
    interface Presenter extends BasePresenter {
    }
}
