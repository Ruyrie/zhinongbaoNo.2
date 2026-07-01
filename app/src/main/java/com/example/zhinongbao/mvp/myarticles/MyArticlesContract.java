package com.example.zhinongbao.mvp.myarticles;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

/**
 * ============================================================
 * 【我的文章 / My Articles】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来显示文章列表、跳转到文章详情。
 *   - Presenter：由 View 调用来处理业务（刷新、取点赞数/评论数/是否已赞、点赞切换）。
 * 数据流向：View → 调 Presenter 方法 → Presenter 操作 ArticleRepository → 回调 View 的 showArticles。
 * 配合的文件：View 实现 = MyArticlesActivity；Presenter 实现 = MyArticlesPresenter；模型 = model/Article。
 * 提示：在 IDE 里搜索「我的文章」可看本组相关文件。
 * ============================================================
 */
public interface MyArticlesContract {
    // View：Presenter 用这些方法控制界面
    interface View extends BaseView<Presenter> {
        void showArticles(List<Article> articles, String currentUser); // 刷新文章列表
        void openArticleDetail(int articleId);                         // 跳转到文章详情
    }

    // Presenter：View（用户点击）用这些方法触发业务处理
    interface Presenter extends BasePresenter {
        void refresh();                          // 重新加载该作者的文章
        int getArticleLikeCount(int articleId);  // 取某文章点赞数
        int getCommentCount(int articleId);      // 取某文章评论数
        boolean isArticleLiked(int articleId);   // 我是否已赞该文章
        void toggleArticleLike(int articleId);   // 点赞/取消点赞
    }
}
