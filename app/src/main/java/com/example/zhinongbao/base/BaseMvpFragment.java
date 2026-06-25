package com.example.zhinongbao.base;

/* ============================================================
 * 【MVP 基础设施 / Base】所有「子页面(Fragment)」的爸爸类（基类）
 * ============================================================
 * 这个文件是干什么的：
 *   和 BaseMvpActivity 几乎一样，只不过它给的是 Fragment（碎片）用的基类。
 *
 * 说明：Activity 和 Fragment 的区别
 *   - Activity = 一整个独立页面。
 *   - Fragment = 「页面里的一块」，可以装进 Activity 里复用。常用于底部导航
 *     切换：主界面(MainActivity)下面有「商城/农技/农友圈/消息/我的」几个 Tab，
 *     每个 Tab 就是一个 Fragment。
 *
 * 提示：在 IDE 里搜索「MVP 基础」可看本组基础类。
 * ============================================================ */

import androidx.fragment.app.Fragment;

// 所有 MVP 子页面(Fragment)的基类：泛型 P 是该子页面的 Presenter 类型
public abstract class BaseMvpFragment<P extends BasePresenter> extends Fragment implements BaseView<P> {
    protected P presenter;   // 业务大脑，子类 Fragment 里直接可用

    // 保存 Presenter
    @Override
    public void setPresenter(P presenter) {
        this.presenter = presenter;
    }
}
