package com.example.zhinongbao.mvp.mycircleposts;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

/**
 * ============================================================
 * 【我的动态 / My Circle Posts】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来刷新界面（显示动态列表、弹提示）。
 *   - Presenter：由 View 调用来处理业务（刷新、清理失效点赞、取各类计数与状态、点赞、关注）。
 * 数据流向：View → 调 Presenter 方法 → Presenter 操作 ArticleRepository → 回调 View 的 showXxx。
 * 配合的文件：View 实现 = MyCirclePostsActivity；Presenter 实现 = MyCirclePostsPresenter；
 *   模型 = model/Article。
 * 提示：在 IDE 里搜索「我的动态」可看本组相关文件。
 * ============================================================
 */
public interface MyCirclePostsContract {
    // View：Presenter 用这些方法把数据「显示」到界面上
    interface View extends BaseView<Presenter> {
        void showPosts(List<Article> posts, String currentUser); // 刷新动态列表
        void showToast(String message);                          // 弹出提示
    }

    // Presenter：View（用户点击）用这些方法触发业务处理
    interface Presenter extends BasePresenter {
        void refresh();                          // 重新加载（我的动态 或 我点赞的动态）
        void clearInvalidPosts();                // 清理已被删除动态的点赞记录
        int getCircleLikeCount(int articleId);   // 取某动态点赞数
        int getCommentCount(int articleId);      // 取某动态评论数
        boolean isCircleLiked(int articleId);    // 我是否已赞该动态
        boolean isFollowing(String author);      // 我是否已关注该作者
        int getUserRole(String username);        // 取用户角色（判断是否卖家）
        void toggleCircleLike(int articleId);    // 点赞/取消点赞
        void followUser(String author);          // 关注作者
    }
}
