package com.example.zhinongbao.mvp.myfavorites;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

/**
 * ============================================================
 * 【我的收藏 / My Favorites】Contract（接口约定）
 * 约定内容：
 *   - View：渲染收藏文章列表（附当前用户名）、弹提示、跳文章详情。
 *   - Presenter：刷新、清理失效文章、查点赞数/评论数/是否已赞、切换点赞。
 * 关键概念：这里的「收藏」= 用户点赞过的文章；列表里的点赞状态与数量由适配器
 *   回调向 Presenter 查询，故有一组 getXxx / toggle 方法。
 * 配合的文件：View 实现 = MyFavoritesActivity；Presenter 实现 = MyFavoritesPresenter；
 *   数据访问 = repository/ArticleRepository；模型 = model/Article。
 * 在 MVP 数据流中的位置：接口层。
 * 提示：在 IDE 里搜索「我的收藏」可看本组相关文件。
 * ============================================================
 */
public interface MyFavoritesContract {
    // View：Presenter 用这些方法更新界面
    interface View extends BaseView<Presenter> {
        void showArticles(List<Article> articles, String currentUser); // 渲染收藏文章列表
        void showToast(String message);       // 弹提示
        void openArticleDetail(int articleId); // 跳转到文章详情
    }

    // Presenter：View（含适配器回调）用这些方法触发业务/查询
    interface Presenter extends BasePresenter {
        void refresh();                          // 重新拉取收藏文章
        void clearInvalidArticles();             // 清理已删除的失效文章
        int getArticleLikeCount(int articleId);  // 查文章点赞数
        int getCommentCount(int articleId);      // 查文章评论数
        boolean isArticleLiked(int articleId);   // 当前用户是否已点赞
        void toggleArticleLike(int articleId);   // 切换点赞/取消点赞
    }
}
