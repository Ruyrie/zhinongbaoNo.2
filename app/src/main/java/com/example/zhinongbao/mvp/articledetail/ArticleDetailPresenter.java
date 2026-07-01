package com.example.zhinongbao.mvp.articledetail;

import android.content.Context;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.Comment;
import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【文章详情 / Article Detail】Presenter（业务逻辑）
 * 整体逻辑：构造时创建 ArticleRepository 并读取当前登录用户；start() 先
 *   把阅读数 +1 再 refresh()；refresh() 取文章数据后依次回调
 *   showArticle / showComments / showLikeState。
 * 数据来源：全部走 ArticleRepository（内部通过 ContentProvider 访问 SQLite），
 *   本类不直接操作数据库。
 * 关键概念：同一个详情页要兼容两类内容 —— 头条「文章」和「农友圈」动态，
 *   二者点赞存在不同的表，用 isCircleArticle() 按 category 区分走不同分支。
 * 配合的文件：接口 = ArticleDetailContract；View = ArticleDetailActivity；
 *   数据访问 = repository/ArticleRepository；模型 = model/Article、model/Comment。
 * 在 MVP 中的位置：Presenter 层。
 * 提示：在 IDE 里搜索「文章详情」可看本组相关文件。
 * ============================================================
 */
public class ArticleDetailPresenter implements ArticleDetailContract.Presenter {
    private final ArticleDetailContract.View view;   // 回调界面
    private final ArticleRepository repository;       // 数据访问入口（走 ContentProvider）
    private final int articleId;                      // 当前文章 id（从上个页面传入）
    private final String currentUser;                 // 当前登录用户名（未登录为 null）
    private Article article;                           // 缓存当前文章对象

    public ArticleDetailPresenter(Context context, ArticleDetailContract.View view, int articleId) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.articleId = articleId;
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    // 页面启动：先把阅读数 +1，再加载全部数据
    @Override
    public void start() {
        repository.incrementReadCount(articleId);
        refresh();
    }

    // 加载文章 + 评论 + 点赞状态；文章不存在（如已被删）则直接关闭页面
    @Override
    public void refresh() {
        article = repository.getArticleById(articleId);
        if (article == null) {
            view.closePage();
            return;
        }
        // 一并把「是否已关注作者」算好传给界面
        view.showArticle(article, currentUser, repository.isFollowing(currentUser, article.author));
        refreshComments();
        refreshLikeState();
    }

    @Override
    public void refreshComments() {
        view.showComments(repository.getComments(articleId, currentUser));
    }

    @Override
    public void refreshLikeState() {
        if (isCircleArticle()) {
            view.showLikeState(repository.isCirclePostLiked(currentUser, articleId),
                    repository.getCirclePostLikeCount(articleId));
        } else {
            view.showLikeState(repository.isArticleLiked(currentUser, articleId),
                    repository.getArticleLikeCount(articleId));
        }
    }

    @Override
    public void deleteArticle() {
        repository.deleteArticle(articleId);
        view.showToast("作品已删除");
        view.closePage();
    }

    @Override
    public void toggleFollow() {
        if (currentUser == null || currentUser.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (article == null || currentUser.equals(article.author)) {
            return;
        }
        if (repository.isFollowing(currentUser, article.author)) {
            repository.unfollowUser(currentUser, article.author);
        } else {
            repository.followUser(currentUser, article.author);
        }
        view.showFollowState(repository.isFollowing(currentUser, article.author));
    }

    @Override
    public void toggleArticleLike() {
        if (currentUser == null || currentUser.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (isCircleArticle()) {
            if (article != null && currentUser.equals(article.author)) {
                repository.unlikeCirclePost(currentUser, articleId);
                view.showToast("不能给自己的动态点赞");
                refreshLikeState();
                return;
            }
            if (repository.isCirclePostLiked(currentUser, articleId)) {
                repository.unlikeCirclePost(currentUser, articleId);
            } else {
                repository.likeCirclePost(currentUser, articleId);
            }
        } else if (repository.isArticleLiked(currentUser, articleId)) {
            repository.unlikeArticle(currentUser, articleId);
        } else if (article != null && currentUser.equals(article.author)) {
            repository.unlikeArticle(currentUser, articleId);
            view.showToast("不能给自己的文章点赞");
        } else {
            repository.likeArticle(currentUser, articleId);
        }
        refreshLikeState();
    }

    @Override
    public void submitComment(String content) {
        if (currentUser == null || currentUser.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (content == null || content.isEmpty()) {
            return;
        }
        repository.addComment(articleId, currentUser, content);
        refreshComments();
    }

    @Override
    public void deleteComment(Comment comment) {
        repository.deleteComment(comment.id);
        refreshComments();
    }

    @Override
    public void likeComment(int commentId) {
        repository.likeComment(currentUser, commentId);
    }

    @Override
    public void unlikeComment(int commentId) {
        repository.unlikeComment(currentUser, commentId);
    }

    // 判断当前内容是否为「农友圈」动态（否则按头条「文章」处理）；
    // 两者点赞存在不同的表，故多处点赞逻辑都要先用它来分流。
    private boolean isCircleArticle() {
        if (article == null) {
            article = repository.getArticleById(articleId);
        }
        return article != null && "农友圈".equals(article.category);
    }
}
