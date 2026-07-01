package com.example.zhinongbao.mvp.agricircle;

import android.content.Context;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.repository.ArticleRepository;

/**
 * ============================================================
 * 【农友圈 / Agri Circle】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1) 构造时创建 ArticleRepository、取出当前登录用户名 currentUser，并 setPresenter 绑定 View。
 *   2) start() 先显示头部头像，再默认加载「最新」动态。
 *   3) loadLatest/loadFollowing/loadMine 分别取全部/关注/我的动态并回调 showPosts。
 *   4) toggleCircleLike 处理点赞：自己的动态只允许取消赞，其余按当前状态切换赞/取消。
 * 数据来源：本类不直接碰数据库，通过 ArticleRepository 访问（Repository 内部经
 *   ContentProvider 访问 SQLite）。
 * 配合的文件：接口 AgriCircleContract；View 实现 AgriCircleFragment；
 *   数据访问 repository/ArticleRepository；模型 model/Article。
 * 在 MVP 数据流中的位置：Presenter 层（View → Presenter → Repository → ContentProvider → SQLite → 回调 View）。
 * 提示：在 IDE 里搜索「农友圈」可看本组相关文件。
 * ============================================================
 */
public class AgriCirclePresenter implements AgriCircleContract.Presenter {
    private final AgriCircleContract.View view;
    private final ArticleRepository repository;
    private final String currentUser;

    public AgriCirclePresenter(Context context, AgriCircleContract.View view) {
        this.view = view;
        this.repository = new ArticleRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.showHeaderAvatar(currentUser, repository.getAvatarUri(currentUser));
        loadLatest();
    }

    @Override
    public void loadLatest() {
        view.showPosts(repository.getCirclePosts(), currentUser);
    }

    @Override
    public void loadFollowing() {
        view.showPosts(repository.getCirclePostsByFollowing(currentUser), currentUser);
    }

    @Override
    public void loadMine() {
        view.showPosts(repository.getCirclePostsByAuthor(currentUser), currentUser);
    }

    @Override
    public int getCircleLikeCount(int articleId) {
        return repository.getCirclePostLikeCount(articleId);
    }

    @Override
    public int getCommentCount(int articleId) {
        return repository.getCommentCount(articleId);
    }

    @Override
    public boolean isCircleLiked(int articleId) {
        return repository.isCirclePostLiked(currentUser, articleId);
    }

    @Override
    public boolean isFollowing(String author) {
        return repository.isFollowing(currentUser, author);
    }

    @Override
    public int getUserRole(String username) {
        return repository.getUserRole(username);
    }

    @Override
    public void toggleCircleLike(int articleId) {
        if (isOwnCirclePost(articleId)) {
            repository.unlikeCirclePost(currentUser, articleId);
            return;
        }
        if (repository.isCirclePostLiked(currentUser, articleId)) {
            repository.unlikeCirclePost(currentUser, articleId);
        } else {
            repository.likeCirclePost(currentUser, articleId);
        }
    }

    @Override
    public void followUser(String author) {
        repository.followUser(currentUser, author);
    }

    private boolean isOwnCirclePost(int articleId) {
        Article article = repository.getArticleById(articleId);
        return article != null && currentUser != null && currentUser.equals(article.author);
    }
}
