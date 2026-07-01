package com.example.zhinongbao.mvp.productcomments;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.ProductComment;

import java.util.List;

/**
 * ============================================================
 * 【商品评价列表 / Product Comments】Contract（接口约定）
 * 约定内容：
 *   - View：渲染评价列表（附带当前用户名以判断哪些可删）、弹提示、跳写评价页。
 *   - Presenter：刷新、点「写评价」（校验是否购买过）、删除评价。
 * 配合的文件：View 实现 = ProductCommentsActivity；Presenter 实现 =
 *   ProductCommentsPresenter；数据访问 = repository/ProductRepository；
 *   模型 = model/ProductComment。
 * 在 MVP 数据流中的位置：接口层。
 * 提示：在 IDE 里搜索「商品评价」可看本组相关文件。
 * ============================================================
 */
public interface ProductCommentsContract {
    // View：Presenter 用这些方法更新评价界面
    interface View extends BaseView<Presenter> {
        void showComments(List<ProductComment> comments, String currentUser); // 渲染评价列表
        void showToast(String message);      // 弹提示
        void openAddComment(int productId);  // 跳转到写评价页
    }

    // Presenter：View（用户操作）用这些方法触发业务
    interface Presenter extends BasePresenter {
        void refresh();                              // 重新拉取评价
        void writeReview();                          // 点「写评价」（先校验购买资格）
        void deleteComment(ProductComment comment);  // 删除某条评价
    }
}
