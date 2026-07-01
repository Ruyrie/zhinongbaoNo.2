package com.example.zhinongbao.mvp.headline;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

/**
 * ============================================================
 * 【头条 / Headline】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来刷新界面（显示文章列表）。
 *   - Presenter：由 View 调用来处理业务（刷新、取点赞数/评论数/是否已赞、点赞切换）。
 * 数据流向：View → 调 Presenter 方法 → Presenter 操作 ArticleRepository → 回调 View 的 showArticles。
 * 配合的文件：View 实现 = HeadlineFragment；Presenter 实现 = HeadlinePresenter；模型 = model/Article。
 * 提示：在 IDE 里搜索「头条」可看本组相关文件。
 * ============================================================
 */
public interface HeadlineContract {
    // View：Presenter 用这些方法把数据「显示」到界面上
    interface View extends BaseView<Presenter> {
        void showArticles(List<Article> articles, String currentUser); // 刷新文章列表
    }

    // Presenter：View（用户点击）用这些方法触发业务处理
    interface Presenter extends BasePresenter {
        void refresh();                          // 重新加载全部文章
        int getArticleLikeCount(int articleId);  // 取某文章点赞数
        int getCommentCount(int articleId);      // 取某文章评论数
        boolean isArticleLiked(int articleId);   // 我是否已赞该文章
        void toggleArticleLike(int articleId);   // 点赞/取消点赞
    }
}
