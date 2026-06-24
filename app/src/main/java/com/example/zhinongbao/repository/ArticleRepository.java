package com.example.zhinongbao.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.Comment;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArticleRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";

    private final Context context;
    private final ContentResolver resolver;

    public ArticleRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
        migrateCircleLikesFromArticleLikes();
    }

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

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

    public List<Article> getCirclePosts() {
        return getArticlesByCategory("农友圈", null, null);
    }

    public List<Article> getCirclePostsByAuthor(String author) {
        return getArticlesByCategory("农友圈", "author=?", new String[] { author });
    }

    public List<Article> getCirclePostsByFollowing(String username) {
        List<Article> articles = new ArrayList<>();
        for (String following : getFollowing(username)) {
            articles.addAll(getCirclePostsByAuthor(following));
        }
        articles.sort((a, b) -> Integer.compare(b.id, a.id));
        return articles;
    }

    public void addCirclePost(String content, String imageUri) {
        String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
        addArticle(title, content, imageUri, "农友圈");
    }

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
                if (!article.isDeleted && "农友圈".equals(article.category)) {
                    continue;
                }
                articles.add(article);
            }
        }
        return articles;
    }

    public void likeArticle(String username, int articleId) {
        if (username == null || username.isEmpty()) {
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

    public void unlikeArticle(String username, int articleId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) });
    }

    public boolean isArticleLiked(String username, int articleId) {
        if (username == null || username.isEmpty()) {
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

    public int getArticleLikeCount(int articleId) {
        return count(ZhiNongBaoProvider.CONTENT_URI_ARTICLE_LIKES,
                "article_id=?",
                new String[] { String.valueOf(articleId) });
    }

    public void likeCirclePost(String username, int articleId) {
        if (username == null || username.isEmpty() || !isCirclePost(articleId)
                || isCirclePostLiked(username, articleId)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("article_id", articleId);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES, values);
    }

    public void unlikeCirclePost(String username, int articleId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) });
    }

    public boolean isCirclePostLiked(String username, int articleId) {
        if (username == null || username.isEmpty()) {
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

    public int getCirclePostLikeCount(int articleId) {
        return count(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_LIKES,
                "article_id=?",
                new String[] { String.valueOf(articleId) });
    }

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

    public void unfavoriteCirclePost(String username, int articleId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CIRCLE_FAVORITES,
                "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) });
    }

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

    public int getCommentCount(int articleId) {
        return count(ZhiNongBaoProvider.CONTENT_URI_COMMENTS,
                "article_id=?",
                new String[] { String.valueOf(articleId) });
    }

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

    public void deleteComment(int commentId) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES,
                "comment_id=?", new String[] { String.valueOf(commentId) });
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_COMMENTS,
                "id=?", new String[] { String.valueOf(commentId) });
    }

    public void likeComment(String username, int commentId) {
        if (username == null || username.isEmpty() || isCommentLiked(username, commentId)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("comment_id", commentId);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES, values);
    }

    public void unlikeComment(String username, int commentId) {
        if (username == null || username.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES,
                "username=? AND comment_id=?",
                new String[] { username, String.valueOf(commentId) });
    }

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

    public void unfollowUser(String follower, String following) {
        if (follower == null || follower.isEmpty()) {
            return;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                "follower=? AND `following`=?",
                new String[] { follower, following });
    }

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

    public List<String> getFollowing(String username) {
        List<String> users = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return users;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                new String[] { "following" },
                "follower=?",
                new String[] { username },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                users.add(cursor.getString(0));
            }
        }
        return users;
    }

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

    public int getFollowersCount(String username) {
        return count(ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                "`following`=?",
                new String[] { username });
    }

    public int getFollowingCount(String username) {
        return count(ZhiNongBaoProvider.CONTENT_URI_FOLLOWS,
                "follower=?",
                new String[] { username });
    }

    public int getTotalLikesReceived(String username) {
        int total = 0;
        for (Article article : getArticlesByAuthor(username)) {
            total += "农友圈".equals(article.category)
                    ? getCirclePostLikeCount(article.id)
                    : getArticleLikeCount(article.id);
        }
        return total;
    }

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

    private boolean isCirclePost(int articleId) {
        Article article = getArticleById(articleId);
        return article != null && "农友圈".equals(article.category);
    }

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

    private int count(android.net.Uri uri, String selection, String[] selectionArgs) {
        try (Cursor cursor = resolver.query(uri, new String[] { "id" }, selection, selectionArgs, null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    private int getCommentLikeCount(int commentId) {
        return count(ZhiNongBaoProvider.CONTENT_URI_COMMENT_LIKES,
                "comment_id=?",
                new String[] { String.valueOf(commentId) });
    }

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

    private String[] articleProjection() {
        return new String[] { "id", "title", "content", "author", "time", "read_count", "cover_uri", "category" };
    }

    private Article cursorToArticle(Cursor cursor) {
        Article article = new Article(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4));
        article.readCount = cursor.getInt(5);
        article.coverUri = cursor.getString(6);
        article.category = cursor.getString(7) == null ? "热点新闻" : cursor.getString(7);
        fillAuthorInfo(article);
        return article;
    }

    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }
}
