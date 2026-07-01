package com.example.zhinongbao.mvp.agritech;

/**
 * ============================================================
 * 【农技学堂 / AgriTech】Presenter（业务逻辑）
 * 整体逻辑：构造时把自己交给 View（setPresenter）；start() 被调用时，
 *   通知 View 去加载本地资产网页 file:///android_asset/agritech.html。
 * 数据来源：地址是写死的本地 HTML5 文件（assets/agritech.html）。
 *   若以后要改成线上资讯，只需把这里的 url 换成 http(s) 网址即可。
 * 配合的文件：接口 = AgriTechContract；View = AgriTechFragment。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「农技学堂」可看本组相关文件。
 * ============================================================
 */
public class AgriTechPresenter implements AgriTechContract.Presenter {
    private final AgriTechContract.View view; // 持有 View 接口，用来回调更新界面

    public AgriTechPresenter(AgriTechContract.View view) {
        this.view = view;
        this.view.setPresenter(this); // 反向绑定：让 View 也拿到本 Presenter
    }

    // 页面启动入口：加载本地农技学堂网页
    @Override
    public void start() {
        view.loadAgriTechPage("file:///android_asset/agritech.html");
    }
}
