package com.example.zhinongbao.mvp.addcirclepost;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【发农友圈动态 / Add Circle Post】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来弹提示、发布成功后关闭页面。
 *   - Presenter：由 View 调用来提交动态（内容 + 图片 URI 串）。
 * 数据流向：View 收集输入 → 调 submit → Presenter 写入 ArticleRepository → 回调 View 提示并关页。
 * 配合的文件：View 实现 = AddCirclePostActivity；Presenter 实现 = AddCirclePostPresenter。
 * 提示：在 IDE 里搜索「发农友圈」可看本组相关文件。
 * ============================================================
 */
public interface AddCirclePostContract {
    // View：Presenter 用这些方法控制界面
    interface View extends BaseView<Presenter> {
        void showToast(String message); // 弹出提示
        void closePage();               // 关闭发布页
    }

    // Presenter：View（用户点发布）用这个方法触发业务处理
    interface Presenter extends BasePresenter {
        void submit(String content, String imageUris); // 提交动态（正文 + 逗号分隔的图片 URI 串）
    }
}
