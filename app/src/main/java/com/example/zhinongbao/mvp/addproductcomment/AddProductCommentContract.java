package com.example.zhinongbao.mvp.addproductcomment;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【发表商品评价 / Add Product Comment】Contract（接口约定）
 * 约定内容：
 *   - View：弹提示、关闭页面。
 *   - Presenter：判断能否评价、给出被拦截时的提示文案、提交评价（文字+图片）。
 * 配合的文件：View 实现 = AddProductCommentActivity；Presenter 实现 =
 *   AddProductCommentPresenter；数据访问 = repository/ProductRepository。
 * 在 MVP 数据流中的位置：接口层。
 * 提示：在 IDE 里搜索「发表评价」可看本组相关文件。
 * ============================================================
 */
public interface AddProductCommentContract {
    // View：Presenter 用这些方法更新界面
    interface View extends BaseView<Presenter> {
        void showToast(String message); // 弹提示
        void closePage();               // 关闭当前页
    }

    // Presenter：View 用这些方法触发业务
    interface Presenter extends BasePresenter {
        boolean canComment();                     // 当前用户是否有资格评价
        String getReviewBlockMessage();           // 无资格时的提示文案
        void submit(String content, String images); // 提交评价（文字 + 逗号分隔的图片 Uri）
    }
}
