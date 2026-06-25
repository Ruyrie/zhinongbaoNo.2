package com.example.zhinongbao.base;

/* ============================================================
 * 【MVP 基础设施 / Base】所有「业务大脑(Presenter)」的总接口
 * ============================================================
 * 这个文件是干什么的：
 *   规定了所有 Presenter 都必须有一个 start() 方法。start() 通常在页面打开时被
 *   调用，用来「第一次加载数据」（比如购物车的 start() 就是去数据库读购物车）。
 *
 * 说明：
 *   - Presenter = 业务大脑，负责思考和处理数据，不碰界面控件。
 *   - 每个 XxxContract.Presenter 都会 extends BasePresenter，所以都带有 start()。
 *
 * 关联：BaseView 是它的另一半（界面）。
 * 提示：在 IDE 里搜索「MVP 基础」可看本组基础类。
 * ============================================================ */

// Presenter（业务大脑）通用接口
public interface BasePresenter {
    void start();   // 页面启动时调用：做首次数据加载/初始化
}
