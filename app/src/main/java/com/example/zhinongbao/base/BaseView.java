package com.example.zhinongbao.base;

/* ============================================================
 * 【MVP 基础设施 / Base】所有「界面(View)」的总接口
 * ============================================================
 * 这个文件是干什么的：
 *   MVP 模式里，每个页面的界面都叫 View。这个接口规定了「所有 View 都必须
 *   能接收一个 Presenter（业务大脑）」——也就是 setPresenter 方法。
 *
 * 说明：
 *   - 泛型 <T>：T 代表「具体某个页面的 Presenter 类型」，比如购物车这里 T = CartContract.Presenter。
 *   - 每个 XxxContract.View 都会 extends BaseView，从而天生带有 setPresenter 能力。
 *
 * 关联：BaseMvpActivity / BaseMvpFragment 实现它；BasePresenter 是它的另一半。
 * 提示：在 IDE 里搜索「MVP 基础」可看本组基础类。
 * ============================================================ */

// View（界面）通用接口：泛型 T 是这个界面对应的 Presenter 类型
public interface BaseView<T> {
    void setPresenter(T presenter);   // 把业务大脑(Presenter)交给界面保存，方便界面回调它
}
