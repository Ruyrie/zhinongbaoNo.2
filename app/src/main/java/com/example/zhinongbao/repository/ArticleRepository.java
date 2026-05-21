package com.example.zhinongbao.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.util.ArrayList;
import java.util.List;

public class ArticleRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";

    private final Context context;
    private final ContentResolver resolver;

    public ArticleRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
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
                articles.add(getArticleOrDeleted(likes.getInt(0)));
            }
        }
        return articles;
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

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }
}
