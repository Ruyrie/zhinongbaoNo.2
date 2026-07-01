package com.example.zhinongbao.mvp.articledetail;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.Comment;

import java.util.List;

/**
 * ============================================================
 * 【文章详情 / Article Detail】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来刷新界面（显示正文、评论、点赞/关注状态、
 *     弹提示、关闭页面）。
 *   - Presenter：由 View（用户操作）调用来处理业务（刷新、删除文章、
 *     关注/取关、点赞、发/删/赞评论）。
 * 数据流向：View → 调 Presenter 方法 → Presenter 操作 ArticleRepository →
 *   再回调 View 的 showXxx 方法更新界面。
 * 配合的文件：View 实现 = ArticleDetailActivity；Presenter 实现 = ArticleDetailPresenter；
 *   模型 = model/Article、model/Comment。
 * 提示：在 IDE 里搜索「文章详情」可看本组相关文件。
 * ============================================================
 */
public interface ArticleDetailContract {
    // View：Presenter 用这些方法把数据「显示」到界面上
    interface View extends BaseView<Presenter> {
        void showArticle(Article article, String currentUser, boolean following); // 渲染正文与作者信息
        void showComments(List<Comment> comments);       // 刷新评论列表
        void showLikeState(boolean liked, int likeCount); // 更新点赞图标与数量
        void showFollowState(boolean following);          // 更新「关注/已关注」按钮
        void showToast(String message);                   // 弹出提示
        void closePage();                                 // 关闭当前详情页
    }

    // Presenter：View（用户点击）用这些方法触发业务处理
    interface Presenter extends BasePresenter {
        void refresh();                    // 重新加载文章 + 评论 + 点赞状态
        void refreshComments();            // 只刷新评论
        void refreshLikeState();           // 只刷新点赞状态
        void deleteArticle();              // 删除本篇文章
        void toggleFollow();               // 关注 / 取消关注作者
        void toggleArticleLike();          // 给文章点赞 / 取消点赞
        void submitComment(String content);// 发表评论
        void deleteComment(Comment comment);// 删除某条评论
        void likeComment(int commentId);   // 给评论点赞
        void unlikeComment(int commentId); // 取消评论点赞
    }
}
