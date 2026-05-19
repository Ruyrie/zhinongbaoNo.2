package com.example.zhinongbao.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.CartItem;
import com.example.zhinongbao.model.Comment;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 统一数据管理类。
 * 全部业务数据（用户/文章/商品/购物车/订单/点赞/评论/关注）存储于 SQLite。
 * 登录会话（logged_user、admin_mode）保留在 SharedPreferences（轻量、无需持久化）。
 */
public class DataManager {

    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";
    private static final String KEY_ADMIN_MODE = "admin_mode";
    private static final String KEY_ACTIVE_ROLE = "active_role";

    private static DataManager instance;
    private final Context ctx;
    private final AppDatabase appDb;

    public static class StoreFootprint {
        public String seller;
        public String storeName;
        public String storePhone;
        public long viewedAt;
        public int productCount;
    }

    private DataManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.appDb = AppDatabase.getInstance(this.ctx);
        seedDefaultData();
    }

    public static DataManager getInstance(Context ctx) {
        if (instance == null)
            instance = new DataManager(ctx);
        return instance;
    }

    // ─── 会话（仍用 SharedPreferences） ─────────────────────────────────────

    public void setLoggedUser(String username) {
        prefs().edit().putString(KEY_LOGGED_USER, username).apply();
    }

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    public void setActiveRole(int role) {
        prefs().edit().putInt(KEY_ACTIVE_ROLE, role).apply();
    }

    public int getActiveRole() {
        return prefs().getInt(KEY_ACTIVE_ROLE, User.ROLE_BUYER);
    }

    public boolean updateUserRole(String username, int newRole) {
        ContentValues cv = new ContentValues();
        cv.put("role", newRole);
        return wdb().update("users", cv, "username=?", new String[] { username }) > 0;
    }

    public int getUserRole(String username) {
        if (username == null)
            return User.ROLE_BUYER;
        try (Cursor c = rdb().rawQuery("SELECT role FROM users WHERE username=?", new String[] { username })) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        }
        return User.ROLE_BUYER;
    }

    public void logout() {
        prefs().edit().remove(KEY_LOGGED_USER).remove(KEY_ACTIVE_ROLE).apply();
    }

    public void setAdminMode(boolean enabled) {
        prefs().edit().putBoolean(KEY_ADMIN_MODE, enabled).apply();
    }

    public boolean isAdminMode() {
        return prefs().getBoolean(KEY_ADMIN_MODE, false);
    }

    public void hideUserFromHistory(String username) {
        java.util.Set<String> hidden = new java.util.HashSet<>(
                prefs().getStringSet("hidden_users", new java.util.HashSet<>()));
        hidden.add(username);
        prefs().edit().putStringSet("hidden_users", hidden).apply();
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    // ─── 用户 ────────────────────────────────────────────────────────────────

    public List<User> getUsers() {
        List<User> list = new ArrayList<>();
        java.util.Set<String> hidden = prefs().getStringSet("hidden_users", new java.util.HashSet<>());
        try (Cursor c = rdb().rawQuery("SELECT username,password,avatar_uri,role FROM users", null)) {
            while (c.moveToNext()) {
                String uName = c.getString(0);
                if (hidden.contains(uName))
                    continue;
                User u = new User(uName, c.getString(1));
                u.avatarUri = c.getString(2);
                u.role = c.getInt(3);
                list.add(u);
            }
        }
        return list;
    }

    /** 注册，用户名唯一，返回 false 表示已存在 */
    public boolean register(String username, String password, String phone) {
        return register(username, password, phone, User.ROLE_BUYER);
    }

    public boolean register(String username, String password, String phone, int role) {
        if (phone != null && !phone.isEmpty()) {
            if (isPhoneBound(phone))
                return false;
        }
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("password", password);
        cv.put("nickname", username);
        cv.put("role", role);
        if (phone != null && !phone.isEmpty()) {
            cv.put("phone", phone);
        }
        return wdb().insertWithOnConflict("users", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    public boolean register(String username, String password) {
        return register(username, password, null);
    }

    public boolean isPhoneBound(String phone) {
        try (Cursor c = rdb().rawQuery("SELECT 1 FROM users WHERE phone=?", new String[] { phone })) {
            return c.moveToFirst();
        }
    }

    public String getPhone(String username) {
        try (Cursor c = rdb().rawQuery("SELECT phone FROM users WHERE username=?", new String[] { username })) {
            if (c.moveToFirst()) {
                String phone = c.getString(0);
                return phone != null ? phone : "";
            }
        }
        return "";
    }

    public boolean updatePhone(String username, String phone) {
        if (isPhoneBound(phone))
            return false;
        ContentValues cv = new ContentValues();
        cv.put("phone", phone);
        return wdb().update("users", cv, "username=?", new String[] { username }) > 0;
    }

    public String getSignature(String username) {
        try (Cursor c = rdb().rawQuery("SELECT signature FROM users WHERE username=?", new String[] { username })) {
            if (c.moveToFirst()) {
                String sig = c.getString(0);
                return sig != null ? sig : "这个人很懒，什么都没留下";
            }
        }
        return "这个人很懒，什么都没留下";
    }

    public User login(String usernameOrPhone, String password) {
        try (Cursor c = rdb().rawQuery(
                "SELECT username,password,role FROM users WHERE (username=? OR phone=?) AND password=?",
                new String[] { usernameOrPhone, usernameOrPhone, password })) {
            if (c.moveToFirst()) {
                String uName = c.getString(0);
                java.util.Set<String> hidden = new java.util.HashSet<>(
                        prefs().getStringSet("hidden_users", new java.util.HashSet<>()));
                if (hidden.remove(uName)) {
                    prefs().edit().putStringSet("hidden_users", hidden).apply();
                }
                User user = new User(uName, c.getString(1));
                user.role = c.getInt(2);
                return user;
            }
        }
        return null;
    }

    public String findUsernameByAccount(String account) {
        try (Cursor c = rdb().rawQuery(
                "SELECT username FROM users WHERE username=? OR phone=?",
                new String[] { account, account })) {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
        }
        return null;
    }

    public boolean isSameAsOldPassword(String username, String password) {
        try (Cursor c = rdb().rawQuery("SELECT password FROM users WHERE username=?", new String[] { username })) {
            if (c.moveToFirst()) {
                String oldPassword = c.getString(0);
                return password.equals(oldPassword);
            }
        }
        return false;
    }

    public boolean changePassword(String username, String newPassword) {
        ContentValues cv = new ContentValues();
        cv.put("password", newPassword);
        return wdb().update("users", cv, "username=?", new String[] { username }) > 0;
    }

    public boolean deleteUser(String username) {
        wdb().delete("users", "username=?", new String[] { username });
        return true;
    }

    // ─── 用户资料 ────────────────────────────────────────────────────────────

    public String getNickname(String username) {
        try (Cursor c = rdb().rawQuery(
                "SELECT nickname FROM users WHERE username=?", new String[] { username })) {
            if (c.moveToFirst()) {
                String n = c.getString(0);
                return (n != null && !n.isEmpty()) ? n : username;
            }
        }
        return username;
    }

    public boolean setNickname(String username, String nickname) {
        ContentValues cv = new ContentValues();
        cv.put("nickname", nickname);
        return wdb().update("users", cv, "username=?", new String[] { username }) > 0;
    }

    public boolean updateSignature(String username, String signature) {
        ContentValues cv = new ContentValues();
        cv.put("signature", signature);
        return wdb().update("users", cv, "username=?", new String[] { username }) > 0;
    }

    public String getAvatarUri(String username) {
        try (Cursor c = rdb().rawQuery(
                "SELECT avatar_uri FROM users WHERE username=?", new String[] { username })) {
            if (c.moveToFirst())
                return c.getString(0);
        }
        return null;
    }

    public boolean setAvatarUri(String username, String uri) {
        ContentValues cv = new ContentValues();
        cv.put("avatar_uri", uri);
        return wdb().update("users", cv, "username=?", new String[] { username }) > 0;
    }

    public String getStoreName(String username) {
        try (Cursor c = rdb().rawQuery("SELECT store_name,nickname FROM users WHERE username=?",
                new String[] { username })) {
            if (c.moveToFirst()) {
                String name = c.getString(0);
                if (name != null && !name.trim().isEmpty())
                    return name;
                String nick = c.getString(1);
                return ((nick != null && !nick.isEmpty()) ? nick : username) + "的店铺";
            }
        }
        return "店铺";
    }

    public String getStorePhone(String username) {
        try (Cursor c = rdb().rawQuery("SELECT store_phone,phone FROM users WHERE username=?",
                new String[] { username })) {
            if (c.moveToFirst()) {
                String storePhone = c.getString(0);
                if (storePhone != null && !storePhone.trim().isEmpty())
                    return storePhone;
                String phone = c.getString(1);
                return phone != null ? phone : "";
            }
        }
        return "";
    }

    public boolean updateStoreInfo(String username, String storeName, String storePhone) {
        ContentValues cv = new ContentValues();
        cv.put("store_name", storeName);
        cv.put("store_phone", storePhone);
        return wdb().update("users", cv, "username=?", new String[] { username }) > 0;
    }

    // ─── 文章 ────────────────────────────────────────────────────────────────

    public List<Article> getArticles() {
        return queryArticles(
                "SELECT a.id,a.title,a.content,a.author,a.time,a.read_count,a.cover_uri,a.category,u.nickname,u.avatar_uri FROM articles a LEFT JOIN users u ON a.author=u.username ORDER BY a.id DESC",
                null);
    }

    public List<Article> getArticlesByAuthor(String author) {
        return queryArticles(
                "SELECT a.id,a.title,a.content,a.author,a.time,a.read_count,a.cover_uri,a.category,u.nickname,u.avatar_uri FROM articles a LEFT JOIN users u ON a.author=u.username WHERE a.author=? ORDER BY a.id DESC",
                new String[] { author });
    }

    public void addArticle(String title, String content, String coverUri, String category) {
        int newId = nextId("articles");
        ContentValues cv = new ContentValues();
        cv.put("id", newId);
        cv.put("title", title);
        cv.put("content", content);
        cv.put("author", getLoggedUser());
        cv.put("time", now("yyyy-MM-dd HH:mm"));
        cv.put("read_count", 0);
        cv.put("category", (category != null && !category.isEmpty()) ? category : "热点新闻");
        if (coverUri != null)
            cv.put("cover_uri", coverUri);
        wdb().insert("articles", null, cv);
    }

    /** 兼容旧调用，category 默认热点新闻 */
    public void addArticle(String title, String content, String coverUri) {
        addArticle(title, content, coverUri, "热点新闻");
    }

    public void incrementReadCount(int articleId) {
        wdb().execSQL("UPDATE articles SET read_count = read_count + 1 WHERE id=?",
                new Object[] { articleId });
    }

    private List<Article> queryArticles(String sql, String[] args) {
        List<Article> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Article a = new Article(c.getInt(0), c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4));
                a.readCount = c.getInt(5);
                a.coverUri = c.getString(6);
                int catIdx = c.getColumnIndex("category");
                a.category = (catIdx >= 0 && c.getString(catIdx) != null)
                        ? c.getString(catIdx)
                        : "热点新闻";
                int nickIdx = c.getColumnIndex("nickname");
                a.authorNickname = (nickIdx >= 0) ? c.getString(nickIdx) : null;
                int avatarIdx = c.getColumnIndex("avatar_uri");
                a.authorAvatarUri = (avatarIdx >= 0) ? c.getString(avatarIdx) : null;
                list.add(a);
            }
        }
        return list;
    }

    // ─── 文章点赞 / 收藏 ─────────────────────────────────────────────────────

    public void likeArticle(String username, int articleId) {
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("article_id", articleId);
        wdb().insertWithOnConflict("article_likes", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void unlikeArticle(String username, int articleId) {
        wdb().delete("article_likes", "username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) });
    }

    public boolean isArticleLiked(String username, int articleId) {
        try (Cursor c = rdb().rawQuery(
                "SELECT 1 FROM article_likes WHERE username=? AND article_id=?",
                new String[] { username, String.valueOf(articleId) })) {
            return c.moveToFirst();
        }
    }

    public int getArticleLikeCount(int articleId) {
        return queryCount("SELECT COUNT(*) FROM article_likes WHERE article_id=?",
                String.valueOf(articleId));
    }

    /** 返回当前用户点赞的文章列表（我的收藏） */
    public List<Article> getLikedArticles(String username) {
        List<Article> list = new ArrayList<>();
        String sql = "SELECT l.article_id, a.title, a.content, a.author, a.time, a.read_count, a.cover_uri, a.category, u.nickname, u.avatar_uri "
                + "FROM article_likes l LEFT JOIN articles a ON l.article_id=a.id "
                + "LEFT JOIN users u ON a.author=u.username "
                + "WHERE l.username=? ORDER BY l.id DESC";
        try (Cursor c = rdb().rawQuery(sql, new String[] { username })) {
            while (c.moveToNext()) {
                int articleId = c.getInt(0);
                String title = c.getString(1);
                Article a;
                if (title == null) {
                    a = new Article(articleId, "该稿件已被删除", "抱歉，该作品已被作者删除。", "", "");
                    a.isDeleted = true;
                } else {
                    a = new Article(articleId, title, c.getString(2), c.getString(3), c.getString(4));
                    a.readCount = c.getInt(5);
                    a.coverUri = c.getString(6);
                    int catIdx = c.getColumnIndex("category");
                    a.category = (catIdx >= 0 && c.getString(catIdx) != null)
                            ? c.getString(catIdx)
                            : "热点新闻";
                    int nickIdx = c.getColumnIndex("nickname");
                    a.authorNickname = (nickIdx >= 0) ? c.getString(nickIdx) : null;
                    int avatarIdx = c.getColumnIndex("avatar_uri");
                    a.authorAvatarUri = (avatarIdx >= 0) ? c.getString(avatarIdx) : null;
                }
                list.add(a);
            }
        }
        return list;
    }

    public int clearInvalidLikedArticles(String username) {
        // 删除那些在 articles 表中不存在对应记录的点赞
        String sql = "article_id NOT IN (SELECT id FROM articles) AND username=?";
        return wdb().delete("article_likes", sql, new String[] { username });
    }

    public void deleteArticle(int articleId) {
        wdb().delete("articles", "id=?", new String[] { String.valueOf(articleId) });
        // 注意：不删除 article_likes 中的记录，以便其他用户能在收藏列表中看到“已被删除”状态
        // 评论等可以删除，或者保留
        wdb().delete("comments", "article_id=?", new String[] { String.valueOf(articleId) });
    }

    // ─── 商品评价 ────────────────────────────────────────────────────────────────

    public boolean hasPurchasedProduct(String username, int productId) {
        if (username == null || username.isEmpty())
            return false;
        // Check if user has an order for this product
        return queryCount("SELECT COUNT(*) FROM orders WHERE username=? AND product_id=?",
                new String[] { username, String.valueOf(productId) }) > 0;
    }

    public com.example.zhinongbao.model.ProductComment addProductComment(int productId, String username, String content,
            String images) {
        String time = now("MM-dd HH:mm");
        ContentValues cv = new ContentValues();
        cv.put("product_id", productId);
        cv.put("username", username);
        cv.put("content", content);
        cv.put("images", images == null ? "" : images);
        cv.put("time", time);
        long id = wdb().insert("product_comments", null, cv);
        return new com.example.zhinongbao.model.ProductComment((int) id, productId, username, content, images, time);
    }

    public List<com.example.zhinongbao.model.ProductComment> getProductComments(int productId) {
        List<com.example.zhinongbao.model.ProductComment> list = new ArrayList<>();
        String sql = "SELECT c.id, c.product_id, c.username, c.content, c.images, c.time, u.nickname, u.avatar_uri " +
                "FROM product_comments c " +
                "LEFT JOIN users u ON c.username=u.username " +
                "WHERE c.product_id=? ORDER BY c.id DESC";
        try (Cursor c = rdb().rawQuery(sql, new String[] { String.valueOf(productId) })) {
            while (c.moveToNext()) {
                com.example.zhinongbao.model.ProductComment pc = new com.example.zhinongbao.model.ProductComment(
                        c.getInt(0), c.getInt(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getString(5));
                pc.nickname = c.getString(6);
                pc.avatarUri = c.getString(7);
                list.add(pc);
            }
        }
        return list;
    }

    public void deleteProductComment(int commentId) {
        wdb().delete("product_comments", "id=?", new String[] { String.valueOf(commentId) });
    }

    public int getProductCommentCount(int productId) {
        return queryCount("SELECT COUNT(*) FROM product_comments WHERE product_id=?",
                String.valueOf(productId));
    }

    // ─── 评论 ────────────────────────────────────────────────────────────────

    public Comment addComment(int articleId, String username, String content) {
        String time = now("MM-dd HH:mm");
        ContentValues cv = new ContentValues();
        cv.put("article_id", articleId);
        cv.put("username", username);
        cv.put("content", content);
        cv.put("time", time);
        long id = wdb().insert("comments", null, cv);
        return new Comment((int) id, articleId, username, content, time, 0);
    }

    public List<Comment> getComments(int articleId, String currentUser) {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT c.id, c.article_id, c.username, c.content, c.time, " +
                "COUNT(cl.id) AS like_count, u.nickname, u.avatar_uri " +
                "FROM comments c " +
                "LEFT JOIN comment_likes cl ON c.id=cl.comment_id " +
                "LEFT JOIN users u ON c.username=u.username " +
                "WHERE c.article_id=? GROUP BY c.id ORDER BY c.id ASC";
        try (Cursor c = rdb().rawQuery(sql, new String[] { String.valueOf(articleId) })) {
            while (c.moveToNext()) {
                Comment cm = new Comment(c.getInt(0), c.getInt(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getInt(5));
                cm.nickname = c.getString(6);
                cm.avatarUri = c.getString(7);
                cm.isLikedByMe = isCommentLiked(currentUser, cm.id);
                list.add(cm);
            }
        }
        return list;
    }

    public void deleteComment(int commentId) {
        wdb().delete("comment_likes", "comment_id=?", new String[] { String.valueOf(commentId) });
        wdb().delete("comments", "id=?", new String[] { String.valueOf(commentId) });
    }

    public int getCommentCount(int articleId) {
        return queryCount("SELECT COUNT(*) FROM comments WHERE article_id=?",
                String.valueOf(articleId));
    }

    // ─── 评论点赞 ────────────────────────────────────────────────────────────

    public void likeComment(String username, int commentId) {
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("comment_id", commentId);
        wdb().insertWithOnConflict("comment_likes", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void unlikeComment(String username, int commentId) {
        wdb().delete("comment_likes", "username=? AND comment_id=?",
                new String[] { username, String.valueOf(commentId) });
    }

    public boolean isCommentLiked(String username, int commentId) {
        try (Cursor c = rdb().rawQuery(
                "SELECT 1 FROM comment_likes WHERE username=? AND comment_id=?",
                new String[] { username, String.valueOf(commentId) })) {
            return c.moveToFirst();
        }
    }

    // ─── 关注 ────────────────────────────────────────────────────────────────

    public void followUser(String follower, String following) {
        ContentValues cv = new ContentValues();
        cv.put("follower", follower);
        cv.put("following", following);
        wdb().insertWithOnConflict("follows", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void unfollowUser(String follower, String following) {
        wdb().delete("follows", "follower=? AND following=?",
                new String[] { follower, following });
    }

    public boolean isFollowing(String follower, String following) {
        try (Cursor c = rdb().rawQuery(
                "SELECT 1 FROM follows WHERE follower=? AND `following`=?",
                new String[] { follower, following })) {
            return c.moveToFirst();
        }
    }

    public int getFollowersCount(String username) {
        return queryCount("SELECT COUNT(*) FROM follows WHERE `following`=?", username);
    }

    public int getFollowingCount(String username) {
        return queryCount("SELECT COUNT(*) FROM follows WHERE follower=?", username);
    }

    /** 返回文章点赞最多的一条评论，用于首页卡片预览；无评论返回 null */
    public Comment getTopComment(int articleId) {
        String sql = "SELECT c.id, c.article_id, c.username, c.content, c.time, " +
                "COUNT(cl.id) AS like_count, u.nickname, u.avatar_uri " +
                "FROM comments c " +
                "LEFT JOIN comment_likes cl ON c.id=cl.comment_id " +
                "LEFT JOIN users u ON c.username=u.username " +
                "WHERE c.article_id=? GROUP BY c.id ORDER BY like_count DESC, c.id DESC LIMIT 1";
        try (Cursor c = rdb().rawQuery(sql, new String[] { String.valueOf(articleId) })) {
            if (c.moveToFirst()) {
                Comment cm = new Comment(c.getInt(0), c.getInt(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getInt(5));
                cm.nickname = c.getString(6);
                cm.avatarUri = c.getString(7);
                return cm;
            }
        }
        return null;
    }

    /** 返回关注 username 的用户名列表（粉丝） */
    public List<String> getFollowers(String username) {
        List<String> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT follower FROM follows WHERE `following`=? ORDER BY id DESC",
                new String[] { username })) {
            while (c.moveToNext())
                list.add(c.getString(0));
        }
        return list;
    }

    /** 返回 username 关注的用户名列表 */
    public List<String> getFollowing(String username) {
        List<String> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT `following` FROM follows WHERE follower=? ORDER BY id DESC",
                new String[] { username })) {
            while (c.moveToNext())
                list.add(c.getString(0));
        }
        return list;
    }

    /** 该用户所有文章累计获得的点赞数 */
    public int getTotalLikesReceived(String username) {
        return queryCount(
                "SELECT COUNT(*) FROM article_likes al " +
                        "INNER JOIN articles a ON al.article_id=a.id WHERE a.author=?",
                username);
    }

    public List<String> getUsersWhoLikedMyArticles(String username) {
        List<String> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT DISTINCT al.username FROM article_likes al " +
                        "INNER JOIN articles a ON al.article_id=a.id WHERE a.author=?",
                new String[] { username })) {
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
        }
        return list;
    }

    // ─── 商品 ────────────────────────────────────────────────────────────────

    public List<Product> getProducts() {
        List<Product> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT id,name,`desc`,price,cover_uri,category,seller,view_count FROM products", null)) {
            while (c.moveToNext()) {
                int catIdx = c.getColumnIndex("category");
                String cat = (catIdx >= 0 && c.getString(catIdx) != null) ? c.getString(catIdx) : "推荐";
                list.add(new Product(c.getInt(0), c.getString(1), c.getString(2), c.getDouble(3),
                        c.getString(4), cat, c.getString(6), c.getInt(7)));
            }
        }
        return list;
    }

    public void addProduct(String name, String desc, double price, String coverUri, String category) {
        int newId = nextId("products");
        ContentValues cv = new ContentValues();
        cv.put("id", newId);
        cv.put("name", name);
        cv.put("desc", desc);
        cv.put("price", price);
        if (coverUri != null) {
            cv.put("cover_uri", coverUri);
        }
        cv.put("category", category != null ? category : "推荐");
        String seller = getLoggedUser();
        if (seller != null)
            cv.put("seller", seller);
        wdb().insert("products", null, cv);
    }

    // 兼容老代码调用
    public void addProduct(String name, String desc, double price, String coverUri) {
        addProduct(name, desc, price, coverUri, "推荐");
    }

    public List<Product> getProductsBySeller(String seller) {
        List<Product> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT id,name,`desc`,price,cover_uri,category,seller,view_count FROM products WHERE seller=?",
                new String[] { seller })) {
            while (c.moveToNext()) {
                int catIdx = c.getColumnIndex("category");
                String cat = (catIdx >= 0 && c.getString(catIdx) != null) ? c.getString(catIdx) : "推荐";
                list.add(new Product(c.getInt(0), c.getString(1), c.getString(2),
                        c.getDouble(3), c.getString(4), cat, c.getString(6), c.getInt(7)));
            }
        }
        return list;
    }

    public void deleteProduct(int productId) {
        wdb().delete("products", "id=?", new String[] { String.valueOf(productId) });
    }

    public boolean updateProduct(int productId, String name, String desc, double price,
            String coverUri, String category) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("desc", desc);
        cv.put("price", price);
        cv.put("category", category != null ? category : "推荐");
        if (coverUri != null && !coverUri.isEmpty())
            cv.put("cover_uri", coverUri);
        return wdb().update("products", cv, "id=?", new String[] { String.valueOf(productId) }) > 0;
    }

    public Product getProductById(int productId) {
        try (Cursor c = rdb().rawQuery(
                "SELECT id,name,`desc`,price,cover_uri,category,seller,view_count FROM products WHERE id=?",
                new String[] { String.valueOf(productId) })) {
            if (c.moveToFirst()) {
                return new Product(c.getInt(0), c.getString(1), c.getString(2),
                        c.getDouble(3), c.getString(4), c.getString(5), c.getString(6), c.getInt(7));
            }
        }
        return null;
    }

    public void recordProductView(String username, int productId) {
        wdb().execSQL("UPDATE products SET view_count = COALESCE(view_count, 0) + 1 WHERE id=?",
                new Object[] { productId });
        if (username == null || username.isEmpty())
            return;
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("product_id", productId);
        cv.put("viewed_at", System.currentTimeMillis());
        wdb().insertWithOnConflict("product_footprints", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getProductViewCount(int productId) {
        return queryCount("SELECT COALESCE(view_count, 0) FROM products WHERE id=?", String.valueOf(productId));
    }

    public List<Product> getProductFootprints(String username) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.id,p.name,p.`desc`,p.price,p.cover_uri,p.category,p.seller,p.view_count,f.viewed_at " +
                "FROM product_footprints f INNER JOIN products p ON f.product_id=p.id " +
                "WHERE f.username=? ORDER BY f.viewed_at DESC";
        try (Cursor c = rdb().rawQuery(sql, new String[] { username })) {
            while (c.moveToNext()) {
                Product p = new Product(c.getInt(0), c.getString(1), c.getString(2),
                        c.getDouble(3), c.getString(4), c.getString(5), c.getString(6), c.getInt(7));
                p.viewedAt = c.getLong(8);
                list.add(p);
            }
        }
        return list;
    }

    public void recordStoreView(String username, String seller) {
        if (username == null || username.isEmpty() || seller == null || seller.isEmpty())
            return;
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("seller", seller);
        cv.put("viewed_at", System.currentTimeMillis());
        wdb().insertWithOnConflict("store_footprints", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<StoreFootprint> getStoreFootprints(String username) {
        List<StoreFootprint> list = new ArrayList<>();
        String sql = "SELECT f.seller,f.viewed_at,u.store_name,u.nickname,u.store_phone,u.phone," +
                "(SELECT COUNT(*) FROM products p WHERE p.seller=f.seller) AS product_count " +
                "FROM store_footprints f LEFT JOIN users u ON f.seller=u.username " +
                "WHERE f.username=? ORDER BY f.viewed_at DESC";
        try (Cursor c = rdb().rawQuery(sql, new String[] { username })) {
            while (c.moveToNext()) {
                StoreFootprint item = new StoreFootprint();
                item.seller = c.getString(0);
                item.viewedAt = c.getLong(1);
                String storeName = c.getString(2);
                String nick = c.getString(3);
                item.storeName = (storeName != null && !storeName.isEmpty())
                        ? storeName
                        : (((nick != null && !nick.isEmpty()) ? nick : item.seller) + "的店铺");
                String storePhone = c.getString(4);
                String phone = c.getString(5);
                item.storePhone = (storePhone != null && !storePhone.isEmpty()) ? storePhone : (phone != null ? phone : "");
                item.productCount = c.getInt(6);
                list.add(item);
            }
        }
        return list;
    }

    public List<Product> getFavoriteProducts(String username) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.id,p.name,p.`desc`,p.price,p.cover_uri,p.category,p.seller,p.view_count " +
                "FROM product_favorites f INNER JOIN products p ON f.product_id=p.id " +
                "WHERE f.username=? ORDER BY f.id DESC";
        try (Cursor c = rdb().rawQuery(sql, new String[] { username })) {
            while (c.moveToNext()) {
                list.add(new Product(c.getInt(0), c.getString(1), c.getString(2),
                        c.getDouble(3), c.getString(4), c.getString(5), c.getString(6), c.getInt(7)));
            }
        }
        return list;
    }

    public int getProductOrderCount(int productId) {
        return queryCount("SELECT COUNT(*) FROM orders WHERE product_id=?",
                String.valueOf(productId));
    }

    public double getProductSalesRevenue(int productId) {
        try (Cursor c = rdb().rawQuery(
                "SELECT COALESCE(SUM(price * quantity), 0.0) FROM orders WHERE product_id=?",
                new String[] { String.valueOf(productId) })) {
            return c.moveToFirst() ? c.getDouble(0) : 0.0;
        }
    }

    public double getTotalRevenueForSeller(String seller) {
        try (Cursor c = rdb().rawQuery(
                "SELECT COALESCE(SUM(o.price * o.quantity), 0.0) FROM orders o " +
                        "INNER JOIN products p ON o.product_id=p.id WHERE p.seller=?",
                new String[] { seller })) {
            return c.moveToFirst() ? c.getDouble(0) : 0.0;
        }
    }

    public int getTotalOrderCountForSeller(String seller) {
        try (Cursor c = rdb().rawQuery(
                "SELECT COALESCE(COUNT(*), 0) FROM orders o " +
                        "INNER JOIN products p ON o.product_id=p.id WHERE p.seller=?",
                new String[] { seller })) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public void deleteConversation(String currentUser, String otherUser) {
        wdb().delete("chat_messages",
                "(from_user=? AND to_user=?) OR (from_user=? AND to_user=?)",
                new String[] { currentUser, otherUser, otherUser, currentUser });
    }

    // ─── 购物车 ───────────────────────────────────────────────────────────────

    public List<CartItem> getCart(String username) {
        List<CartItem> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT product_id,name,price,quantity FROM cart WHERE username=?",
                new String[] { username })) {
            while (c.moveToNext())
                list.add(new CartItem(c.getInt(0), c.getString(1), c.getDouble(2), c.getInt(3)));
        }
        return list;
    }

    public void addToCart(String username, Product product) {
        try (Cursor c = rdb().rawQuery(
                "SELECT quantity FROM cart WHERE username=? AND product_id=?",
                new String[] { username, String.valueOf(product.id) })) {
            if (c.moveToFirst()) {
                int qty = c.getInt(0) + 1;
                wdb().execSQL("UPDATE cart SET quantity=? WHERE username=? AND product_id=?",
                        new Object[] { qty, username, product.id });
            } else {
                ContentValues cv = new ContentValues();
                cv.put("username", username);
                cv.put("product_id", product.id);
                cv.put("name", product.name);
                cv.put("price", product.price);
                cv.put("quantity", 1);
                wdb().insert("cart", null, cv);
            }
        }
    }

    public void saveCartPublic(String username, List<CartItem> items) {
        SQLiteDatabase d = wdb();
        d.delete("cart", "username=?", new String[] { username });
        for (CartItem item : items) {
            ContentValues cv = new ContentValues();
            cv.put("username", username);
            cv.put("product_id", item.productId);
            cv.put("name", item.name);
            cv.put("price", item.price);
            cv.put("quantity", item.quantity);
            d.insert("cart", null, cv);
        }
    }

    // ─── 订单 ────────────────────────────────────────────────────────────────

    public List<Order> getOrders(String username) {
        List<Order> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT order_id,product_id,name,price,quantity,time,status " +
                        "FROM orders WHERE username=? ORDER BY id DESC",
                new String[] { username })) {
            while (c.moveToNext())
                list.add(new Order(c.getString(0), c.getInt(1), c.getString(2),
                        c.getDouble(3), c.getInt(4), c.getString(5), c.getString(6)));
        }
        return list;
    }

    public void addOrder(String username, int productId, String name, double price, int quantity) {
        String seller = null;
        try (Cursor c = rdb().rawQuery("SELECT seller FROM products WHERE id=?",
                new String[] { String.valueOf(productId) })) {
            if (c.moveToFirst())
                seller = c.getString(0);
        }
        ContentValues cv = new ContentValues();
        cv.put("order_id", "JN" + System.currentTimeMillis());
        cv.put("username", username);
        cv.put("product_id", productId);
        cv.put("name", name);
        cv.put("price", price);
        cv.put("quantity", quantity);
        cv.put("time", now("yyyy-MM-dd HH:mm"));
        cv.put("status", Order.STATUS_PENDING);
        cv.put("order_type", Order.ORDER_TYPE_RETAIL);
        if (seller != null)
            cv.put("seller", seller);
        wdb().insert("orders", null, cv);
    }

    public void updateOrderStatus(String username, String orderId, String status) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        wdb().update("orders", cv, "username=? AND order_id=?",
                new String[] { username, orderId });
    }

    public Order getOrderById(String orderId) {
        try (Cursor c = rdb().rawQuery(
                "SELECT o.order_id,o.product_id,o.name,o.price,o.quantity,o.time,o.status," +
                        "o.seller,o.username,o.order_type,o.purchase_request_id," +
                        "o.ship_type,o.ship_name,o.ship_no,o.ship_phone,o.proof_images," +
                        "o.unit_price,o.discount,o.refund_amount,o.refund_reason," +
                        "u.nickname FROM orders o LEFT JOIN users u ON o.username=u.username " +
                        "WHERE o.order_id=?",
                new String[] { orderId })) {
            if (c.moveToFirst())
                return cursorToFullOrder(c);
        }
        return null;
    }

    private Order cursorToFullOrder(Cursor c) {
        Order o = new Order(c.getString(0), c.getInt(1), c.getString(2),
                c.getDouble(3), c.getInt(4), c.getString(5), c.getString(6));
        o.seller = c.getString(7);
        o.buyerUser = c.getString(8);
        o.orderType = c.getString(9);
        o.purchaseRequestId = c.getLong(10);
        o.shipType = c.getString(11);
        o.shipName = c.getString(12);
        o.shipNo = c.getString(13);
        o.shipPhone = c.getString(14);
        o.proofImages = c.getString(15);
        o.unitPrice = c.getDouble(16);
        o.discount = c.getDouble(17);
        o.refundAmount = c.getDouble(18);
        o.refundReason = c.getString(19);
        o.buyerNickname = c.getColumnCount() > 20 ? c.getString(20) : o.buyerUser;
        if (o.buyerNickname == null || o.buyerNickname.isEmpty())
            o.buyerNickname = o.buyerUser;
        if (o.orderType == null)
            o.orderType = Order.ORDER_TYPE_RETAIL;
        return o;
    }

    public List<Order> getSellerSoldOrders(String seller) {
        List<Order> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT o.order_id,o.product_id,o.name,o.price,o.quantity,o.time,o.status," +
                        "o.seller,o.username,o.order_type,o.purchase_request_id," +
                        "o.ship_type,o.ship_name,o.ship_no,o.ship_phone,o.proof_images," +
                        "o.unit_price,o.discount,o.refund_amount,o.refund_reason," +
                        "u.nickname FROM orders o LEFT JOIN users u ON o.username=u.username " +
                        "WHERE o.seller=? ORDER BY o.id DESC",
                new String[] { seller })) {
            while (c.moveToNext())
                list.add(cursorToFullOrder(c));
        }
        return list;
    }

    public List<Order> getSellerSoldOrdersByStatus(String seller, String status) {
        List<Order> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT o.order_id,o.product_id,o.name,o.price,o.quantity,o.time,o.status," +
                        "o.seller,o.username,o.order_type,o.purchase_request_id," +
                        "o.ship_type,o.ship_name,o.ship_no,o.ship_phone,o.proof_images," +
                        "o.unit_price,o.discount,o.refund_amount,o.refund_reason," +
                        "u.nickname FROM orders o LEFT JOIN users u ON o.username=u.username " +
                        "WHERE o.seller=? AND o.status=? ORDER BY o.id DESC",
                new String[] { seller, status })) {
            while (c.moveToNext())
                list.add(cursorToFullOrder(c));
        }
        return list;
    }

    public boolean shipOrder(String orderId, String shipType, String shipName,
            String shipNo, String shipPhone) {
        ContentValues cv = new ContentValues();
        cv.put("status", Order.STATUS_SHIPPED);
        cv.put("ship_type", shipType);
        cv.put("ship_name", shipName);
        cv.put("ship_no", shipNo);
        cv.put("ship_phone", shipPhone);
        return wdb().update("orders", cv, "order_id=?", new String[] { orderId }) > 0;
    }

    public boolean updateOrderPrice(String orderId, double unitPrice, double discount) {
        ContentValues cv = new ContentValues();
        cv.put("unit_price", unitPrice);
        cv.put("discount", discount);
        return wdb().update("orders", cv, "order_id=?", new String[] { orderId }) > 0;
    }

    public boolean initiatePartialRefund(String orderId, double amount, String reason) {
        ContentValues cv = new ContentValues();
        cv.put("refund_amount", amount);
        cv.put("refund_reason", reason);
        cv.put("status", Order.STATUS_REFUND);
        return wdb().update("orders", cv, "order_id=?", new String[] { orderId }) > 0;
    }

    public boolean processRefund(String orderId, double amount, String reason, boolean approve) {
        ContentValues cv = new ContentValues();
        cv.put("refund_amount", amount);
        cv.put("refund_reason", reason);
        cv.put("status", approve ? Order.STATUS_COMPLETED : Order.STATUS_SHIPPED);
        return wdb().update("orders", cv, "order_id=?", new String[] { orderId }) > 0;
    }

    public boolean updateOrderProofImages(String orderId, String images) {
        ContentValues cv = new ContentValues();
        cv.put("proof_images", images);
        return wdb().update("orders", cv, "order_id=?", new String[] { orderId }) > 0;
    }

    public void updateOrderStatusByOrderId(String orderId, String status) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        wdb().update("orders", cv, "order_id=?", new String[] { orderId });
    }

    // ─── 工具方法 ─────────────────────────────────────────────────────────────

    private SQLiteDatabase rdb() {
        return appDb.getReadableDatabase();
    }

    private SQLiteDatabase wdb() {
        return appDb.getWritableDatabase();
    }

    private int queryCount(String sql, String arg) {
        try (Cursor c = rdb().rawQuery(sql, new String[] { arg })) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private int queryCount(String sql, String[] args) {
        try (Cursor c = rdb().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private int nextId(String table) {
        try (Cursor c = rdb().rawQuery(
                "SELECT COALESCE(MAX(id),0)+1 FROM " + table, null)) {
            return c.moveToFirst() ? c.getInt(0) : 1;
        }
    }

    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    // ─── 种子数据（DB 空时初始化） ────────────────────────────────────────────

    private void seedDefaultData() {
        // 确保能刷新出 10 件初始商品和 5 篇文章
        boolean needSeed = false;
        try (Cursor c = rdb().rawQuery("SELECT COUNT(*) FROM products", null)) {
            if (c.moveToFirst() && c.getInt(0) < 10) {
                needSeed = true;
            }
        }
        if (!needSeed)
            return;

        SQLiteDatabase d = wdb();
        // 清除旧数据，避免主键冲突
        d.execSQL("DELETE FROM articles WHERE id <= 5");
        d.execSQL("DELETE FROM products WHERE id <= 10");

        register("admin", "123456");
        register("user1", "123456");
        register("user2", "123456");
        register("user3", "123456");
        register("user4", "123456");

        // 示例文章 (5个不同用户发布，覆盖不同分类)
        insertArticle(d, 5, "吉林科技学院举办科技节",
                "本次科技节汇聚了来自全国各地的农业科技专家，展示了最新农业技术成果，吸引了众多师生参与。展览期间，多项智慧农业设备首次亮相，引起了广泛关注。专家们对这些技术的实际应用前景进行了深入探讨。",
                "admin", "2026-04-20 09:00", 15, "热点新闻");
        insertArticle(d, 4, "新型水稻品种研发成功",
                "经过多年培育，我校农学院成功研发出高产、抗病新型水稻品种，亩产可达800公斤以上，为粮食安全提供有力保障。该品种在抗倒伏和抗病虫害方面表现优异，有望在下个种植季大面积推广。",
                "user1", "2026-04-15 14:30", 8, "专家咨询");
        insertArticle(d, 3, "智慧农业实验基地投入使用",
                "学校智慧农业实验基地正式投入使用，基地配备物联网传感器、无人机等先进设备，开创农业教育新模式。学生们现在可以在基地进行实地操作，将理论知识与现代农业技术完美结合。",
                "user2", "2026-04-10 10:00", 12, "支农宝新闻");
        insertArticle(d, 2, "农业经济论坛成功举办",
                "本届农业经济论坛围绕乡村振兴战略展开深入讨论，多位专家学者分享了最新研究成果和政策解读。会议指出，特色农产品品牌化和电商化将是未来农村经济发展的重要驱动力。",
                "user3", "2026-04-05 16:00", 5, "创业项目");
        insertArticle(d, 1, "长白山野生菌类采摘季开启",
                "随着雨季的到来，长白山地区的野生菌类迎来了丰收季。当地农户严格遵守可持续采摘原则，确保生态平衡。同时，新鲜的羊肚菌、鹿茸菇等珍稀菌类已开始陆续供应市场。",
                "user4", "2026-04-01 08:30", 20, "热点新闻");

        // 示例商品
        insertProduct(d, 1, "东北大米（5kg）",
                "精选东北优质长粒香米，颗粒饱满，口感软糯，自然种植，无添加。", 45.00, "推荐,米面粮油");
        insertProduct(d, 2, "有机黑木耳（250g）",
                "长白山纯天然有机黑木耳，肉厚脆嫩，富含多糖及铁元素，营养丰富。", 38.50, "推荐,农资农具");
        insertProduct(d, 3, "农家蜂蜜（500g）",
                "纯天然百花蜂蜜，无任何添加剂，每瓶均经过质量检测，香甜可口。", 68.00, "推荐");
        insertProduct(d, 4, "绿色蔬菜礼盒",
                "精选时令新鲜蔬菜组合，产自有机农场，当日采摘，新鲜直达。", 99.00, "推荐,水果蔬菜");
        insertProduct(d, 5, "优质冬虫夏草（10g）",
                "精选高原正宗冬虫夏草，根条饱满，色泽金黄，滋补佳品，送礼自用两相宜。", 880.00, "推荐");
        insertProduct(d, 6, "农家红薯（5kg）",
                "沙地种植红心红薯，软糯香甜，富含膳食纤维，健康代餐好选择。", 29.90, "水果蔬菜");
        insertProduct(d, 7, "新鲜铁棍山药（2.5kg）",
                "正宗温县铁棍山药，质地细腻，口感绵甜，营养价值极高，煲汤佳品。", 55.00, "水果蔬菜");
        insertProduct(d, 8, "野生羊肚菌（100g）",
                "深山野生采摘羊肚菌，香味浓郁，肉质厚实，炖汤鲜美无比，营养滋补。", 128.00, "农资农具");
        insertProduct(d, 9, "鲜货鹿茸菇（250g）",
                "新鲜采摘鹿茸菇，口感脆滑，香味独特，富含多种氨基酸，适合炒菜或炖汤。", 45.00, "农资农具");
        insertProduct(d, 10, "散养土鹅蛋（10枚）",
                "农家林地散养大白鹅产蛋，蛋黄大而橙红，营养丰富，天然无公害。", 65.00, "推荐,米面粮油");
    }

    private void insertArticle(SQLiteDatabase d, int id, String title, String content,
            String author, String time, int readCount, String category) {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("title", title);
        cv.put("content", content);
        cv.put("author", author);
        cv.put("time", time);
        cv.put("read_count", readCount);
        cv.put("category", category != null ? category : "热点新闻");
        d.insertWithOnConflict("articles", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertProduct(SQLiteDatabase d, int id, String name, String desc, double price) {
        insertProduct(d, id, name, desc, price, "推荐");
    }

    private void insertProduct(SQLiteDatabase d, int id, String name, String desc, double price, String category) {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("name", name);
        cv.put("desc", desc);
        cv.put("price", price);
        cv.put("category", category);
        d.insertWithOnConflict("products", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // ─── 农友圈 ──────────────────────────────────────────────────────────────

    private static final String CIRCLE_SQL = "SELECT a.id,a.title,a.content,a.author,a.time,a.read_count,a.cover_uri,a.category,"
            +
            "u.nickname,u.avatar_uri FROM articles a LEFT JOIN users u ON a.author=u.username ";

    /** 全部农友圈动态（最新优先） */
    public List<Article> getCirclePosts() {
        return queryArticles(CIRCLE_SQL + "WHERE a.category='农友圈' ORDER BY a.id DESC", null);
    }

    /** 关注用户的农友圈动态 */
    public List<Article> getCirclePostsByFollowing(String username) {
        return queryArticles(CIRCLE_SQL +
                "WHERE a.author IN (SELECT `following` FROM follows WHERE follower=?) " +
                "AND a.category='农友圈' ORDER BY a.id DESC", new String[] { username });
    }

    /** 指定用户的农友圈动态 */
    public List<Article> getCirclePostsByAuthor(String username) {
        return queryArticles(CIRCLE_SQL +
                "WHERE a.author=? AND a.category='农友圈' ORDER BY a.id DESC",
                new String[] { username });
    }

    /** 发布农友圈动态（无标题，内容即正文） */
    public void addCirclePost(String content, String imageUri) {
        String title = content.length() > 30 ? content.substring(0, 30) + "…" : content;
        addArticle(title, content, imageUri, "农友圈");
    }

    // ─── 聊天消息 ─────────────────────────────────────────────────────────────

    public void sendMessage(String fromUser, String toUser, String content) {
        ContentValues cv = new ContentValues();
        cv.put("from_user", fromUser);
        cv.put("to_user", toUser);
        cv.put("content", content);
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("is_read", 0);
        wdb().insert("chat_messages", null, cv);
    }

    public List<com.example.zhinongbao.model.ChatMessage> getMessages(String user1, String user2) {
        List<com.example.zhinongbao.model.ChatMessage> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT id,from_user,to_user,content,timestamp,is_read FROM chat_messages " +
                        "WHERE (from_user=? AND to_user=?) OR (from_user=? AND to_user=?) " +
                        "ORDER BY timestamp ASC",
                new String[] { user1, user2, user2, user1 })) {
            while (c.moveToNext()) {
                com.example.zhinongbao.model.ChatMessage m = new com.example.zhinongbao.model.ChatMessage();
                m.id = c.getLong(0);
                m.fromUser = c.getString(1);
                m.toUser = c.getString(2);
                m.content = c.getString(3);
                m.timestamp = c.getLong(4);
                m.isRead = c.getInt(5) == 1;
                list.add(m);
            }
        }
        return list;
    }

    public List<com.example.zhinongbao.model.ConversationItem> getConversations(String username) {
        List<com.example.zhinongbao.model.ConversationItem> list = new ArrayList<>();
        java.util.LinkedHashMap<String, com.example.zhinongbao.model.ConversationItem> map = new java.util.LinkedHashMap<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT from_user,to_user,content,timestamp FROM chat_messages " +
                        "WHERE from_user=? OR to_user=? ORDER BY timestamp DESC",
                new String[] { username, username })) {
            while (c.moveToNext()) {
                String from = c.getString(0);
                String to = c.getString(1);
                String other = from.equals(username) ? to : from;
                if (!map.containsKey(other)) {
                    com.example.zhinongbao.model.ConversationItem item = new com.example.zhinongbao.model.ConversationItem();
                    item.otherUser = other;
                    item.displayName = getNickname(other);
                    item.lastMessage = c.getString(2);
                    item.lastTimestamp = c.getLong(3);
                    item.unreadCount = getUnreadCount(username, other);
                    map.put(other, item);
                }
            }
        }
        list.addAll(map.values());
        return list;
    }

    public void markMessagesRead(String fromUser, String toUser) {
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        wdb().update("chat_messages", cv, "from_user=? AND to_user=?", new String[] { fromUser, toUser });
    }

    public int getUnreadCount(String toUser, String fromUser) {
        try (Cursor c = rdb().rawQuery(
                "SELECT COUNT(*) FROM chat_messages WHERE to_user=? AND from_user=? AND is_read=0",
                new String[] { toUser, fromUser })) {
            if (c.moveToFirst())
                return c.getInt(0);
        }
        return 0;
    }

    public int getTotalUnreadMessageCount(String username) {
        try (Cursor c = rdb().rawQuery(
                "SELECT COUNT(*) FROM chat_messages WHERE to_user=? AND is_read=0",
                new String[] { username })) {
            if (c.moveToFirst())
                return c.getInt(0);
        }
        return 0;
    }

    // ─── 采购需求 ─────────────────────────────────────────────────────────────

    public boolean addPurchaseRequest(String productName, String category, double quantity,
            String unit, double targetPrice, String description) {
        String buyer = getLoggedUser();
        if (buyer == null)
            return false;
        ContentValues cv = new ContentValues();
        cv.put("buyer_user", buyer);
        cv.put("product_name", productName);
        cv.put("category", category);
        cv.put("quantity", quantity);
        cv.put("unit", unit);
        cv.put("target_price", targetPrice);
        cv.put("description", description);
        cv.put("timestamp", System.currentTimeMillis());
        return wdb().insert("purchase_requests", null, cv) != -1;
    }

    public List<com.example.zhinongbao.model.PurchaseRequest> getPurchaseRequests() {
        List<com.example.zhinongbao.model.PurchaseRequest> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT r.id, r.buyer_user, r.product_name, r.category, r.quantity, r.unit," +
                        " r.target_price, r.description, r.timestamp, u.nickname," +
                        " (SELECT COUNT(*) FROM purchase_quotes WHERE request_id=r.id) AS quote_count" +
                        " FROM purchase_requests r LEFT JOIN users u ON r.buyer_user=u.username" +
                        " ORDER BY r.timestamp DESC",
                null)) {
            while (c.moveToNext()) {
                com.example.zhinongbao.model.PurchaseRequest req = new com.example.zhinongbao.model.PurchaseRequest();
                req.id = c.getLong(0);
                req.buyerUser = c.getString(1);
                req.productName = c.getString(2);
                req.category = c.getString(3);
                req.quantity = c.getDouble(4);
                req.unit = c.getString(5);
                req.targetPrice = c.getDouble(6);
                req.description = c.getString(7);
                req.timestamp = c.getLong(8);
                req.buyerNickname = c.getString(9);
                if (req.buyerNickname == null || req.buyerNickname.isEmpty())
                    req.buyerNickname = req.buyerUser;
                req.quoteCount = c.getInt(10);
                list.add(req);
            }
        }
        return list;
    }

    public List<com.example.zhinongbao.model.PurchaseRequest> getMyPurchaseRequests(String username) {
        List<com.example.zhinongbao.model.PurchaseRequest> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT r.id, r.buyer_user, r.product_name, r.category, r.quantity, r.unit," +
                        " r.target_price, r.description, r.timestamp, u.nickname," +
                        " (SELECT COUNT(*) FROM purchase_quotes WHERE request_id=r.id) AS quote_count" +
                        " FROM purchase_requests r LEFT JOIN users u ON r.buyer_user=u.username" +
                        " WHERE r.buyer_user=? ORDER BY r.timestamp DESC",
                new String[] { username })) {
            while (c.moveToNext()) {
                com.example.zhinongbao.model.PurchaseRequest req = new com.example.zhinongbao.model.PurchaseRequest();
                req.id = c.getLong(0);
                req.buyerUser = c.getString(1);
                req.productName = c.getString(2);
                req.category = c.getString(3);
                req.quantity = c.getDouble(4);
                req.unit = c.getString(5);
                req.targetPrice = c.getDouble(6);
                req.description = c.getString(7);
                req.timestamp = c.getLong(8);
                req.buyerNickname = c.getString(9);
                if (req.buyerNickname == null || req.buyerNickname.isEmpty())
                    req.buyerNickname = req.buyerUser;
                req.quoteCount = c.getInt(10);
                list.add(req);
            }
        }
        return list;
    }

    public boolean addQuote(long requestId, String sellerUser, double price, String description) {
        ContentValues cv = new ContentValues();
        cv.put("request_id", requestId);
        cv.put("seller_user", sellerUser);
        cv.put("price", price);
        cv.put("description", description);
        cv.put("timestamp", System.currentTimeMillis());
        return wdb().insertWithOnConflict("purchase_quotes", null, cv, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public List<com.example.zhinongbao.model.PurchaseQuote> getQuotesForRequest(long requestId) {
        List<com.example.zhinongbao.model.PurchaseQuote> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT q.id, q.request_id, q.seller_user, q.price, q.description, q.timestamp, u.nickname" +
                        " FROM purchase_quotes q LEFT JOIN users u ON q.seller_user=u.username" +
                        " WHERE q.request_id=? ORDER BY q.timestamp ASC",
                new String[] { String.valueOf(requestId) })) {
            while (c.moveToNext()) {
                com.example.zhinongbao.model.PurchaseQuote quote = new com.example.zhinongbao.model.PurchaseQuote();
                quote.id = c.getLong(0);
                quote.requestId = c.getLong(1);
                quote.sellerUser = c.getString(2);
                quote.price = c.getDouble(3);
                quote.description = c.getString(4);
                quote.timestamp = c.getLong(5);
                quote.sellerNickname = c.getString(6);
                if (quote.sellerNickname == null || quote.sellerNickname.isEmpty())
                    quote.sellerNickname = quote.sellerUser;
                list.add(quote);
            }
        }
        return list;
    }

    public boolean hasQuoted(long requestId, String sellerUser) {
        try (Cursor c = rdb().rawQuery(
                "SELECT 1 FROM purchase_quotes WHERE request_id=? AND seller_user=?",
                new String[] { String.valueOf(requestId), sellerUser })) {
            return c.moveToFirst();
        }
    }

    /** 卖家查看自己提交的所有报价（含状态和买家回复） */
    public List<com.example.zhinongbao.model.PurchaseQuote> getQuotesBySellerUser(String sellerUser) {
        List<com.example.zhinongbao.model.PurchaseQuote> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT q.id, q.request_id, q.seller_user, q.price, q.description, q.timestamp," +
                        " u.nickname, q.status, q.reply_desc, r.product_name" +
                        " FROM purchase_quotes q" +
                        " LEFT JOIN users u ON q.seller_user=u.username" +
                        " LEFT JOIN purchase_requests r ON q.request_id=r.id" +
                        " WHERE q.seller_user=? ORDER BY q.timestamp DESC",
                new String[] { sellerUser })) {
            while (c.moveToNext()) {
                com.example.zhinongbao.model.PurchaseQuote quote = new com.example.zhinongbao.model.PurchaseQuote();
                quote.id = c.getLong(0);
                quote.requestId = c.getLong(1);
                quote.sellerUser = c.getString(2);
                quote.price = c.getDouble(3);
                quote.description = c.getString(4);
                quote.timestamp = c.getLong(5);
                quote.sellerNickname = c.getString(6);
                quote.status = c.getString(7);
                quote.replyDesc = c.getString(8);
                quote.requestProductName = c.getString(9);
                if (quote.sellerNickname == null || quote.sellerNickname.isEmpty())
                    quote.sellerNickname = quote.sellerUser;
                if (quote.status == null)
                    quote.status = "pending";
                list.add(quote);
            }
        }
        return list;
    }

    /** 接受报价：更新状态并创建 procurement 订单 */
    public boolean acceptQuote(long quoteId, String replyDesc) {
        ContentValues cv = new ContentValues();
        cv.put("status", "accepted");
        cv.put("reply_desc", replyDesc);
        boolean ok = wdb().update("purchase_quotes", cv, "id=?",
                new String[] { String.valueOf(quoteId) }) > 0;
        if (!ok)
            return false;

        // 查询报价详情以创建订单
        try (Cursor c = rdb().rawQuery(
                "SELECT q.request_id, q.seller_user, q.price, r.buyer_user, r.product_name, r.quantity" +
                        " FROM purchase_quotes q LEFT JOIN purchase_requests r ON q.request_id=r.id" +
                        " WHERE q.id=?",
                new String[] { String.valueOf(quoteId) })) {
            if (c.moveToFirst()) {
                long reqId = c.getLong(0);
                String sellerUser = c.getString(1);
                double price = c.getDouble(2);
                String buyerUser = c.getString(3);
                String productName = c.getString(4);
                int qty = (int) c.getDouble(5);

                ContentValues ocv = new ContentValues();
                ocv.put("order_id", "JN" + System.currentTimeMillis());
                ocv.put("username", buyerUser);
                ocv.put("product_id", 0);
                ocv.put("name", productName);
                ocv.put("price", price);
                ocv.put("quantity", qty);
                ocv.put("time", now("yyyy-MM-dd HH:mm"));
                ocv.put("status", Order.STATUS_PENDING);
                ocv.put("order_type", Order.ORDER_TYPE_PROCUREMENT);
                ocv.put("purchase_request_id", reqId);
                ocv.put("seller", sellerUser);
                wdb().insert("orders", null, ocv);
            }
        }
        return true;
    }

    /** 拒绝报价 */
    public boolean rejectQuote(long quoteId, String replyDesc) {
        ContentValues cv = new ContentValues();
        cv.put("status", "rejected");
        cv.put("reply_desc", replyDesc);
        return wdb().update("purchase_quotes", cv, "id=?",
                new String[] { String.valueOf(quoteId) }) > 0;
    }

    /** 获取某采购需求的报价列表（含状态） */
    public List<com.example.zhinongbao.model.PurchaseQuote> getQuotesForRequestWithStatus(long requestId) {
        List<com.example.zhinongbao.model.PurchaseQuote> list = new ArrayList<>();
        try (Cursor c = rdb().rawQuery(
                "SELECT q.id, q.request_id, q.seller_user, q.price, q.description, q.timestamp," +
                        " u.nickname, q.status, q.reply_desc" +
                        " FROM purchase_quotes q LEFT JOIN users u ON q.seller_user=u.username" +
                        " WHERE q.request_id=? ORDER BY q.timestamp ASC",
                new String[] { String.valueOf(requestId) })) {
            while (c.moveToNext()) {
                com.example.zhinongbao.model.PurchaseQuote quote = new com.example.zhinongbao.model.PurchaseQuote();
                quote.id = c.getLong(0);
                quote.requestId = c.getLong(1);
                quote.sellerUser = c.getString(2);
                quote.price = c.getDouble(3);
                quote.description = c.getString(4);
                quote.timestamp = c.getLong(5);
                quote.sellerNickname = c.getString(6);
                quote.status = c.getString(7);
                quote.replyDesc = c.getString(8);
                if (quote.sellerNickname == null || quote.sellerNickname.isEmpty())
                    quote.sellerNickname = quote.sellerUser;
                if (quote.status == null)
                    quote.status = "pending";
                list.add(quote);
            }
        }
        return list;
    }
}
