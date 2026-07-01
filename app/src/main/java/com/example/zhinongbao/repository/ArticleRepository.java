package com.example.zhinongbao.repository;

/* ============================================================
 * 【文章 / 农技学堂 / 头条 / 农友圈 / Article】数据仓库（Repository）
 * ============================================================
 * （靠 category 区分：「农友圈」是社交动态，其它分类是资讯文章）：
 *   - 资讯文章（头条/农技学堂）：增删改查、阅读量+1、点赞、收藏。
 *   - 农友圈动态：发布、按作者/关注查看、点赞、收藏。
 *   - 评论：发表、查看、删除、评论点赞。
 *   - 关注关系：关注/取关、查粉丝/关注列表及数量、统计获赞数。
 *
 * 技术点：
 *   - 一表多用：文章和农友圈动态都在 articles 表，category=「农友圈」即为动态。
 *   - 点赞分两张表：article_likes（资讯）和 circle_likes（农友圈），互不混淆；
 *     构造方法里的 migrateCircleLikesFromArticleLikes 把历史遗留的农友圈点赞迁到正确的表。
 *   - getArticleOrDeleted：作者删了文章后，点赞/收藏列表里用「该稿件已被删除」占位，不报错。
 *   - articleProjection + cursorToArticle 统一取列与转换；fillAuthorInfo 补作者昵称头像。
 *
 * 谁在用它：头条/农技学堂/农友圈/文章详情/我的文章/我的动态/收藏/关注列表 等 Presenter。
 * 提示：在 IDE 里搜索「文章」「农友圈」「评论」「关注」可看相关文件。
 * ============================================================ */

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.Comment;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArticleRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";
    private static final String STORE_FOLLOW_PREFIX = "shop:";
    private static final String KEY_LEGACY_STORE_FOLLOWS_MIGRATED = "legacy_store_follows_migrated";

    private final Context context;
    private final ContentResolver resolver;

    public ArticleRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
        migrateLegacyStoreFollows();
        migrateCircleLikesFromArticleLikes();   // 创建时顺手做一次历史点赞数据迁移
    }

    // 取当前登录用户名
    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    // 取全部资讯文章（最新在前）
    public List<Article> getArticles() {
        List<Article> articles = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLES,
                articleProjection(),
                null,
                null,
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                articles.add(cursorToArticle(cursor));
            }
        }
        return articles;
    }

    // 取某作者发布的全部内容（「我的文章」用）
    public List<Article> getArticlesByAuthor(String author) {
        List<Article> articles = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLES,
                articleProjection(),
                "author=?",
                new String[] { author },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                articles.add(cursorToArticle(cursor));
            }
        }
        return articles;
    }

    // 取农友圈全部动态（category=农友圈）
    public List<Article> getCirclePosts() {
        return getArticlesByCategory("农友圈", null, null);
    }

    // 取某作者的农友圈动态（「我的动态」用）
    public List<Article> getCirclePostsByAuthor(String author) {
        return getArticlesByCategory("农友圈", "author=?", new String[] { author });
    }

    // 取「我关注的人」发布的农友圈动态，合并后按 id（即时间）倒序
    public List<Article> getCirclePostsByFollowing(String username) {
        List<Article> articles = new ArrayList<>();
        for (String following : getFollowing(username)) {
            articles.addAll(getCirclePostsByAuthor(following));
        }
        articles.sort((a, b) -> Integer.compare(b.id, a.id));
        return articles;
    }

    // 发布一条农友圈动态：取正文前 30 字当标题，分类固定为「农友圈」
    public void addCirclePost(String content, String imageUri) {
        String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
        addArticle(title, content, imageUri, "农友圈");
    }

    // 按 id 查单篇文章/动态
    public Article getArticleById(int articleId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLES,
                articleProjection(),
                "id=?",
                new String[] { String.valueOf(articleId) },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToArticle(cursor);
            }
        }
        return null;
    }

    // 新增一篇文章/动态（作者=当前登录用户，时间=现在，分类缺省「热点新闻」）
    public void addArticle(String title, String content, String coverUri, String category) {
        ContentValues values = new ContentValues();
        values.put("id", nextArticleId());
        values.put("title", title);
        values.put("content", content);
        values.put("author", getLoggedUser());
        values.put("time", now("yyyy-MM-dd HH:mm"));
        values.put("read_count", 0);
        values.put("category", category == null || category.isEmpty() ? "热点新闻" : category);
        if (coverUri != null) {
            values.put("cover_uri", coverUri);
        }
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ARTICLES, values);
    }

    // 阅读量 +1（打开文章详情时调用）
    public void incrementReadCount(int articleId) {
        Article article = getArticleById(articleId);
        if (article == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("read_count", article.readCount + 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_ARTICLES, values,
                "id=?", new String[] { String.valueOf(articleId) });
    }

    // 删除文章/动态：连同它的评论、各类点赞、收藏一并删除（避免留下孤儿数据）
    public void deleteArticle(int articleId) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_ARTICLES,
                "id=?", new String[] { String.valueOf(articleId) });
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_COMMENTS,
                "article_id=?", new String[] { String.valueOf(articleId) });
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                "article_id=?", new String[] { String.valueOf(articleId) });
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                "article_id=?", new String[] { String.valueOf(articleId) });
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_FAVORITES,
                "article_id=?", new String[] { String.valueOf(articleId) });
    }

    // 取「我点赞过的资讯文章」（农友圈动态的点赞不算在内）
    public List<Article> getLikedArticles(String username) {
        List<Article> articles = new ArrayList<>();
        try (Cursor likes = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                new String[] { "article_id" },
                "username=?",
                new String[] { username },
                "id DESC")) {
            while (likes != null && likes.moveToNext()) {
                Article article = getArticleOrDeleted(likes.getInt(0));
                // 农友圈动态的点赞不计入「文章收藏」，仅展示资讯文章
                if (!article.isDeleted && ("农友圈".equals(article.category)
                        || username.equals(article.author))) {
                    continue;
                }
                articles.add(article);
            }
        }
        return articles;
    }

    // 给资讯文章点赞（已赞过则不重复）
    public void likeArticle(String username, int articleId) {
        if (username == null || username.isEmpty() || isOwnArticle(username, articleId)) {
            return;
        }
        if (isArticleLiked(username, articleId)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("article_id", articleId);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES, values);
    }

    // 取消资讯文章点赞
    public void unlikeArticle(String username, int articleId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) });
    }

    // 判断我是否点赞过该资讯文章
    public boolean isArticleLiked(String username, int articleId) {
        if (username == null || username.isEmpty() || isOwnArticle(username, articleId)) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                new String[] { "id" },
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // 统计某资讯文章的点赞数
    public int getArticleLikeCount(int articleId) {
        Article article = getArticleById(articleId);
        if (article != null && article.author != null && !article.author.isEmpty()) {
            return count(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                    "article_id=? AND username<>?",
                    new String[] { String.valueOf(articleId), article.author });
        }
        return count(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                "article_id=?",
                new String[] { String.valueOf(articleId) });
    }

    // 给农友圈动态点赞（存到 circle_likes 表；非动态或已赞则忽略）
    public void likeCirclePost(String username, int articleId) {
        if (username == null || username.isEmpty() || !isCirclePost(articleId)
                || isOwnArticle(username, articleId) || isCirclePostLiked(username, articleId)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("article_id", articleId);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES, values);
    }

    // 取消农友圈动态点赞
    public void unlikeCirclePost(String username, int articleId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) });
    }

    // 判断我是否点赞过该农友圈动态
    public boolean isCirclePostLiked(String username, int articleId) {
        if (username == null || username.isEmpty() || isOwnArticle(username, articleId)) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                new String[] { "id" },
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // 统计某农友圈动态的点赞数
    public int getCirclePostLikeCount(int articleId) {
        Article article = getArticleById(articleId);
        if (article != null && article.author != null && !article.author.isEmpty()) {
            return count(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                    "article_id=? AND username<>?",
                    new String[] { String.valueOf(articleId), article.author });
        }
        return count(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                "article_id=?",
                new String[] { String.valueOf(articleId) });
    }

    // 收藏农友圈动态
    public void favoriteCirclePost(String username, int articleId) {
        if (username == null || username.isEmpty() || !isCirclePost(articleId)
                || isCirclePostFavorited(username, articleId)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("article_id", articleId);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_FAVORITES, values);
    }

    // 取消收藏农友圈动态
    public void unfavoriteCirclePost(String username, int articleId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_FAVORITES,
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) });
    }

    // 判断我是否收藏过该农友圈动态
    public boolean isCirclePostFavorited(String username, int articleId) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CIRCLE_FAVORITES,
                new String[] { "id" },
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // 取「我收藏的农友圈动态」列表
    public List<Article> getFavoriteCirclePosts(String username) {
        List<Article> articles = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return articles;
        }
        try (Cursor favorites = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CIRCLE_FAVORITES,
                new String[] { "article_id" },
                "username=?",
                new String[] { username },
                "id DESC")) {
            while (favorites != null && favorites.moveToNext()) {
                Article article = getArticleOrDeleted(favorites.getInt(0));
                if (!article.isDeleted && "农友圈".equals(article.category)) {
                    articles.add(article);
                }
            }
        }
        return articles;
    }

    // 统计某文章/动态的评论数
    public int getCommentCount(int articleId) {
        return count(ZhiNongBaoProvider.CONTENT_URI_COMMENTS,
                "article_id=?",
                new String[] { String.valueOf(articleId) });
    }

    // 发表评论：插入后取回自增 id，组装成 Comment 对象（带评论者昵称头像）返回，方便界面直接显示
    public Comment addComment(int articleId, String username, String content) {
        String time = now("MM-dd HH:mm");
        ContentValues values = new ContentValues();
        values.put("article_id", articleId);
        values.put("username", username);
        values.put("content", content);
        values.put("time", time);
        android.net.Uri uri = resolver.insert(ZhiNongBaoProvider.CONTENT_URI_COMMENTS, values);
        int id = uri == null ? 0 : Integer.parseInt(uri.getLastPathSegment());
        Comment comment = new Comment(id, articleId, username, content, time, 0);
        fillCommentUserInfo(comment);
        return comment;
    }

    // 取某文章的全部评论（含每条点赞数、我是否点过赞、评论者昵称头像）
    public List<Comment> getComments(int articleId, String currentUser) {
        List<Comment> comments = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_COMMENTS,
                new String[] { "id", "article_id", "username", "content", "time" },
                "article_id=?",
                new String[] { String.valueOf(articleId) },
                "id ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                Comment comment = new Comment(cursor.getInt(0), cursor.getInt(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), getCommentLikeCount(cursor.getInt(0)));
                comment.isLikedByMe = isCommentLiked(currentUser, comment.id);
                fillCommentUserInfo(comment);
                comments.add(comment);
            }
        }
        return comments;
    }

    // 删除评论：连同它的点赞一起删
    public void deleteComment(int commentId) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES,
                "comment_id=?", new String[] { String.valueOf(commentId) });
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_COMMENTS,
                "id=?", new String[] { String.valueOf(commentId) });
    }

    // 给评论点赞（已赞过则忽略）
    public void likeComment(String username, int commentId) {
        if (username == null || username.isEmpty() || isCommentLiked(username, commentId)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("comment_id", commentId);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES, values);
    }

    // 取消评论点赞
    public void unlikeComment(String username, int commentId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES,
                "username=? AND comment_id=?",
                new String[] { username, String.valueOf(commentId) });
    }

    // 判断我是否点赞过该评论
    public boolean isCommentLiked(String username, int commentId) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES,
                new String[] { "id" },
                "username=? AND comment_id=?",
                new String[] { username, String.valueOf(commentId) },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // 关注某人（follower 关注 following；已关注则忽略）
    public void followUser(String follower, String following) {
        if (follower == null || follower.isEmpty() || following == null || following.isEmpty()
                || isFollowing(follower, following)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("follower", follower);
        values.put("following", following);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_FOLLOWS, values);
    }

    // 取关（following 字段是 SQL 关键字，所以用反引号 `following` 包起来）
    public void unfollowUser(String follower, String following) {
        if (follower == null || follower.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                "follower=? AND `following`=?",
                new String[] { follower, following });
    }

    // 判断 follower 是否已关注 following
    public boolean isFollowing(String follower, String following) {
        if (follower == null || follower.isEmpty() || following == null || following.isEmpty()) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                new String[] { "id" },
                "follower=? AND `following`=?",
                new String[] { follower, following },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // 店铺关注和用户关注共用 follows 表，但用前缀隔离，避免影响学堂/农友圈的用户关注状态
    public void followStore(String follower, String seller) {
        followUser(follower, storeFollowKey(seller));
    }

    public void unfollowStore(String follower, String seller) {
        unfollowUser(follower, storeFollowKey(seller));
    }

    public boolean isFollowingStore(String follower, String seller) {
        return isFollowing(follower, storeFollowKey(seller));
    }

    // 取「我关注的店铺」卖家用户名列表
    public List<String> getFollowingStores(String username) {
        List<String> stores = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return stores;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                new String[] { "following" },
                "follower=? AND `following` LIKE ?",
                new String[] { username, STORE_FOLLOW_PREFIX + "%" },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                String following = cursor.getString(0);
                String seller = storeFollowSeller(following);
                if (!seller.isEmpty() && !stores.contains(seller)) {
                    stores.add(seller);
                }
            }
        }
        return stores;
    }

    // 取「我关注的人」用户名列表
    public List<String> getFollowing(String username) {
        List<String> users = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return users;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                new String[] { "following" },
                "follower=? AND `following` NOT LIKE ?",
                new String[] { username, STORE_FOLLOW_PREFIX + "%" },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                users.add(cursor.getString(0));
            }
        }
        return users;
    }

    // 取「关注我的人（粉丝）」用户名列表
    public List<String> getFollowers(String username) {
        List<String> users = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return users;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                new String[] { "follower" },
                "`following`=?",
                new String[] { username },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                users.add(cursor.getString(0));
            }
        }
        return users;
    }

    // 粉丝数
    public int getFollowersCount(String username) {
        return count(ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                "`following`=?",
                new String[] { username });
    }

    // 关注数：个人页展示总关注数，包含用户关注和店铺关注
    public int getFollowingCount(String username) {
        return count(ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                "follower=?",
                new String[] { username });
    }

    // 统计「我获得的总点赞数」：遍历我发布的全部内容，资讯和动态分别按各自点赞表累加
    public int getTotalLikesReceived(String username) {
        int total = 0;
        for (Article article : getArticlesByAuthor(username)) {
            total += "农友圈".equals(article.category)
                    ? getCirclePostLikeCount(article.id)
                    : getArticleLikeCount(article.id);
        }
        return total;
    }

    // 取「给我文章点过赞的人」用户名列表（去重）。SQL 用子查询找出我作为作者的文章。
    public List<String> getUsersWhoLikedArticlesBy(String username) {
        List<String> users = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return users;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                new String[] { "username" },
                "article_id IN (SELECT id FROM articles WHERE author=?)",
                new String[] { username },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                String user = cursor.getString(0);
                if (!users.contains(user)) {
                    users.add(user);
                }
            }
        }
        return users;
    }

    // 取某用户头像（农友圈/详情页显示作者头像用）
    public String getAvatarUri(String username) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "avatar_uri" },
                "username=?",
                new String[] { username },
                null)) {
            return cursor != null && cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    // 取某用户角色（用于显示卖家标识等）
    public int getUserRole(String username) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "role" },
                "username=?",
                new String[] { username },
                null)) {
            return cursor != null && cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    // 清理「点赞了但文章已不存在」的资讯点赞脏数据，返回清理条数
    public int clearInvalidLikedArticles(String username) {
        int cleared = 0;
        try (Cursor likes = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                new String[] { "article_id" },
                "username=?",
                new String[] { username },
                null)) {
            while (likes != null && likes.moveToNext()) {
                int articleId = likes.getInt(0);
                if (!articleExists(articleId)) {
                    cleared += resolver.delete(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                            "username=? AND article_id=?",
                            new String[] { username, String.valueOf(articleId) });
                }
            }
        }
        return cleared;
    }

    /** 获取用户点赞的农友圈动态；已被作者删除的动态保留为占位提示 */
    public List<Article> getLikedCirclePosts(String username) {
        List<Article> articles = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return articles;
        }
        try (Cursor likes = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                new String[] { "article_id" },
                "username=?",
                new String[] { username },
                "id DESC")) {
            while (likes != null && likes.moveToNext()) {
                Article article = getArticleOrDeleted(likes.getInt(0));
                if (article.isDeleted || ("农友圈".equals(article.category)
                        && !username.equals(article.author))) {
                    articles.add(article);
                }
            }
        }
        return articles;
    }

    /** 清理点赞列表中作者已删除的农友圈动态，返回清理条数 */
    public int clearInvalidCircleLikes(String username) {
        int cleared = 0;
        if (username == null || username.isEmpty()) {
            return cleared;
        }
        try (Cursor likes = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                new String[] { "article_id" },
                "username=?",
                new String[] { username },
                null)) {
            while (likes != null && likes.moveToNext()) {
                int articleId = likes.getInt(0);
                if (!articleExists(articleId)) {
                    cleared += resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                            "username=? AND article_id=?",
                            new String[] { username, String.valueOf(articleId) });
                }
            }
        }
        return cleared;
    }

    // 按 id 取文章；若已被删除则返回一个「该稿件已被删除」的占位 Article（isDeleted=true）
    private Article getArticleOrDeleted(int articleId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLES,
                new String[] { "id", "title", "content", "author", "time", "read_count", "cover_uri", "category" },
                "id=?",
                new String[] { String.valueOf(articleId) },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                Article article = new Article(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4));
                article.readCount = cursor.getInt(5);
                article.coverUri = cursor.getString(6);
                article.category = cursor.getString(7) == null ? "热点新闻" : cursor.getString(7);
                fillAuthorInfo(article);
                return article;
            }
        }
        Article deleted = new Article(articleId, "该稿件已被删除", "抱歉，该作品已被作者删除。", "", "");
        deleted.isDeleted = true;
        return deleted;
    }

    // 判断某文章是否还存在
    private boolean articleExists(int articleId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLES,
                new String[] { "id" },
                "id=?",
                new String[] { String.valueOf(articleId) },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // 判断某条内容是不是农友圈动态
    private boolean isCirclePost(int articleId) {
        Article article = getArticleById(articleId);
        return article != null && "农友圈".equals(article.category);
    }

    private boolean isOwnArticle(String username, int articleId) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        Article article = getArticleById(articleId);
        return article != null && username.equals(article.author);
    }

    // 一次性数据迁移：把历史上误存在 article_likes 表里的「农友圈点赞」搬到 circle_likes 表再删掉。
    // 用 try/catch 包住，迁移失败也不影响 App 正常使用。
    private void migrateCircleLikesFromArticleLikes() {
        try (Cursor likes = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                new String[] { "username", "article_id" },
                "article_id IN (SELECT id FROM articles WHERE category=?)",
                new String[] { "农友圈" },
                null)) {
            while (likes != null && likes.moveToNext()) {
                ContentValues values = new ContentValues();
                values.put("username", likes.getString(0));
                values.put("article_id", likes.getInt(1));
                resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES, values);
            }
        } catch (Exception ignored) {
        }
        try {
            resolver.delete(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                    "article_id IN (SELECT id FROM articles WHERE category=?)",
                    new String[] { "农友圈" });
        } catch (Exception ignored) {
        }
    }

    // 通用计数：查某表满足条件的行数（点赞数、评论数等都靠它）
    private int count(android.net.Uri uri, String selection, String[] selectionArgs) {
        try (Cursor cursor = resolver.query(uri, new String[] { "id" }, selection, selectionArgs, null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    // 某评论的点赞数
    private int getCommentLikeCount(int commentId) {
        return count(ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES,
                "comment_id=?",
                new String[] { String.valueOf(commentId) });
    }

    // 给文章补上作者的昵称和头像（从 users 表查）
    private void fillAuthorInfo(Article article) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "nickname", "avatar_uri" },
                "username=?",
                new String[] { article.author },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                article.authorNickname = cursor.getString(0);
                article.authorAvatarUri = cursor.getString(1);
            }
        }
    }

    // 按分类查文章，并可叠加额外条件（如再限定 author）。动态拼接 SQL 条件和参数。
    private List<Article> getArticlesByCategory(String category, String extraSelection, String[] extraArgs) {
        List<Article> articles = new ArrayList<>();
        String selection = "category=?";
        List<String> args = new ArrayList<>();
        args.add(category);
        if (extraSelection != null && !extraSelection.isEmpty()) {
            selection += " AND " + extraSelection;
            if (extraArgs != null) {
                for (String arg : extraArgs) {
                    args.add(arg);
                }
            }
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLES,
                articleProjection(),
                selection,
                args.toArray(new String[0]),
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                articles.add(cursorToArticle(cursor));
            }
        }
        return articles;
    }

    // 给评论补上评论者的昵称和头像
    private void fillCommentUserInfo(Comment comment) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "nickname", "avatar_uri" },
                "username=?",
                new String[] { comment.username },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                comment.nickname = cursor.getString(0);
                comment.avatarUri = cursor.getString(1);
            }
        }
    }

    // 生成下一个文章 id = 当前最大 id + 1（没有则从 1 开始）
    private int nextArticleId() {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ARTICLES,
                new String[] { "id" },
                null,
                null,
                "id DESC")) {
            return cursor != null && cursor.moveToFirst() ? cursor.getInt(0) + 1 : 1;
        }
    }

    // 查询文章要取的列名（配合 cursorToArticle 按列号取值）
    private String[] articleProjection() {
        return new String[] { "id", "title", "content", "author", "time", "read_count", "cover_uri", "category" };
    }

    // 把查询结果当前一行翻译成 Article 对象，并补上作者信息
    private Article cursorToArticle(Cursor cursor) {
        Article article = new Article(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4));
        article.readCount = cursor.getInt(5);
        article.coverUri = cursor.getString(6);
        article.category = cursor.getString(7) == null ? "热点新闻" : cursor.getString(7);
        fillAuthorInfo(article);
        return article;
    }

    // 按格式返回当前时间字符串
    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    private String storeFollowKey(String seller) {
        return seller == null ? "" : STORE_FOLLOW_PREFIX + seller;
    }

    private String storeFollowSeller(String following) {
        if (following == null || !following.startsWith(STORE_FOLLOW_PREFIX)) {
            return "";
        }
        return following.substring(STORE_FOLLOW_PREFIX.length());
    }

    private void migrateLegacyStoreFollows() {
        if (prefs().getBoolean(KEY_LEGACY_STORE_FOLLOWS_MIGRATED, false)) {
            return;
        }
        List<String[]> legacyRows = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                new String[] { "follower", "following" },
                "`following` NOT LIKE ?",
                new String[] { STORE_FOLLOW_PREFIX + "%" },
                null)) {
            while (cursor != null && cursor.moveToNext()) {
                String follower = cursor.getString(0);
                String following = cursor.getString(1);
                if (isSellerAccount(following)) {
                    legacyRows.add(new String[] { follower, following });
                }
            }
        }
        for (String[] row : legacyRows) {
            followStore(row[0], row[1]);
            unfollowUser(row[0], row[1]);
        }
        prefs().edit().putBoolean(KEY_LEGACY_STORE_FOLLOWS_MIGRATED, true).apply();
    }

    private boolean isSellerAccount(String username) {
        int role = getUserRole(username);
        return role == User.ROLE_SELLER || role == User.ROLE_BOTH;
    }
}
