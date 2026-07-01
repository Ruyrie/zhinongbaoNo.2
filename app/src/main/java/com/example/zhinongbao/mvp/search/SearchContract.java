package com.example.zhinongbao.mvp.search;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

/**
 * ============================================================
 * 【头条搜索 / Search（文章）】Contract（接口约定）
 * 约定内容：View（展示全部文章供本地筛选）；Presenter（查文章点赞数、评论数、
 *   是否已赞、切换点赞）。
 * 配合的文件：View 实现 = SearchActivity；Presenter 实现 = SearchPresenter；
 *   数据访问 = repository/ArticleRepository；模型 = model/Article。
 * 提示：在 IDE 里搜索「头条搜索」可看本组相关文件。
 * ============================================================
 */
public interface SearchContract {
    interface View extends BaseView<Presenter> {
        void showAllArticles(List<Article> articles, String currentUser); // 给出全部文章供搜索筛选
    }

    interface Presenter extends BasePresenter {
        int getArticleLikeCount(int articleId); // 文章点赞数
        int getCommentCount(int articleId);     // 文章评论数
        boolean isArticleLiked(int articleId);  // 当前用户是否已赞
        void toggleArticleLike(int articleId);  // 点赞 / 取消点赞
    }
}
