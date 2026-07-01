package com.example.zhinongbao.mvp.agricircle;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Article;

import java.util.List;

/**
 * ============================================================
 * 【农友圈 / Agri Circle】Contract（接口约定）
 * 约定内容：
 *   - View：由 Presenter 调用来刷新界面（显示动态列表、显示头部头像）。
 *   - Presenter：由 View（用户操作）调用来处理业务（按最新/关注/我的加载、
 *     取点赞数/评论数/是否已赞/是否关注/用户角色、点赞、关注）。
 * 数据流向：View → 调 Presenter 方法 → Presenter 操作 ArticleRepository → 回调 View 的 showXxx。
 * 配合的文件：View 实现 = AgriCircleFragment；Presenter 实现 = AgriCirclePresenter；
 *   模型 = model/Article。
 * 提示：在 IDE 里搜索「农友圈」可看本组相关文件。
 * ============================================================
 */
public interface AgriCircleContract {
    // View：Presenter 用这些方法把数据「显示」到界面上
    interface View extends BaseView<Presenter> {
        void showPosts(List<Article> posts, String currentUser); // 刷新动态列表
        void showHeaderAvatar(String currentUser, String avatarUri); // 显示顶部当前用户头像
    }

    // Presenter：View（用户点击）用这些方法触发业务处理
    interface Presenter extends BasePresenter {
        void loadLatest();                       // 加载「最新」动态
        void loadFollowing();                    // 加载「关注」的人的动态
        void loadMine();                         // 加载「我的」动态
        int getCircleLikeCount(int articleId);   // 取某动态点赞数
        int getCommentCount(int articleId);      // 取某动态评论数
        boolean isCircleLiked(int articleId);    // 我是否已赞该动态
        boolean isFollowing(String author);      // 我是否已关注该作者
        int getUserRole(String username);        // 取用户角色（判断是否卖家以显示进店）
        void toggleCircleLike(int articleId);    // 点赞/取消点赞
        void followUser(String author);          // 关注作者
    }
}
