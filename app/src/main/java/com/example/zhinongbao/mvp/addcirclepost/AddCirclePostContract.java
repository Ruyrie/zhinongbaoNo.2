package com.example.zhinongbao.mvp.addcirclepost;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【发农友圈动态 / Add Circle Post】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来弹提示、（编辑模式）回填原动态、成功后关闭页面。
 *   - Presenter：由 View 调用来发布新动态、加载待编辑动态、保存编辑（内容 + 图片 URI 串）。
 * 关键概念：同一页面既用于「发布新动态」也用于「编辑已有动态」，靠 postId 是否 >0 区分
 *   （与「发布/编辑采购需求」PostPurchase 的做法一致）。
 * 数据流向：View 收集输入 → 调 submit/submitEdit → Presenter 写入/更新 ArticleRepository → 回调 View 提示并关页。
 * 配合的文件：View 实现 = AddCirclePostActivity；Presenter 实现 = AddCirclePostPresenter；
 *   编辑入口 = ArticleDetailActivity（自己的农友圈动态详情页）；数据访问 = repository/ArticleRepository。
 * 提示：在 IDE 里搜索「发农友圈」可看本组相关文件。
 * ============================================================
 */
public interface AddCirclePostContract {
    // View：Presenter 用这些方法控制界面
    interface View extends BaseView<Presenter> {
        void showToast(String message); // 弹出提示
        void closePage();               // 关闭发布页
        void showExistingPost(String content, String imageUris); // 编辑模式：把原动态内容与图片回填到表单
    }

    // Presenter：View（用户点发布/保存）用这些方法触发业务处理
    interface Presenter extends BasePresenter {
        void submit(String content, String imageUris);                 // 发布新动态（正文 + 逗号分隔的图片 URI 串）
        void loadPost(int postId);                                     // 加载待编辑的动态并回填表单
        void submitEdit(int postId, String content, String imageUris); // 保存对已有动态的编辑
    }
}
