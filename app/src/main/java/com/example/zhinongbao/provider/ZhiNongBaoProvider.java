package com.example.zhinongbao.provider;

/* ============================================================
 * 【数据库 / 数据总入口 / Provider】全 App 数据读写的统一大门（ContentProvider）
 * ============================================================
 * 这个文件是干什么的：
 *   它是「数据库的统一对外接口」。所有 Repository 想读写数据，都不直接碰 SQLite，
 *   而是通过 ContentResolver 调用这里的 query/insert/update/delete。
 *   好处：读写入口统一、数据一变就能自动通知界面刷新。
 *
 * 核心概念：
 *   - ContentProvider：安卓四大组件之一，专门「对外提供数据访问」。
 *   - Uri（统一资源定位）：每张表对应一个地址，如
 *       content://com.example.zhinongbao.provider/cart  → 购物车表
 *     代码里用 CONTENT_URI_CART 这样的常量表示，方便调用。
 *   - UriMatcher：一个「地址识别器」，把传进来的 Uri 匹配成编号，再翻译成表名。
 *   - notifyChange：数据改动后「广播通知」，订阅了该 Uri 的界面会自动重新查询刷新。
 *
 * 配套：表结构由 AppDatabase 建立；各 Repository 通过本类间接读写各业务表。
 * 提示：在 IDE 里搜索「数据库」或「Provider」可看数据层文件。
 * ============================================================ */

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zhinongbao.data.AppDatabase;

public class ZhiNongBaoProvider extends ContentProvider {

    // AUTHORITY = 本 Provider 的唯一标识（相当于「门牌号」），所有 Uri 都以它开头
    public static final String AUTHORITY = "com.example.zhinongbao.provider";
    // ↓ 下面每个常量就是「一张表的访问地址」，Repository 直接引用它们来读写对应的表

    public static final Uri CONTENT_URI_USERS = Uri.parse("content://" + AUTHORITY + "/users");
    public static final Uri CONTENT_URI_ARTICLES = Uri.parse("content://" + AUTHORITY + "/articles");
    public static final Uri CONTENT_URI_PRODUCTS = Uri.parse("content://" + AUTHORITY + "/products");
    public static final Uri CONTENT_URI_CART = Uri.parse("content://" + AUTHORITY + "/cart");
    public static final Uri CONTENT_URI_ORDERS = Uri.parse("content://" + AUTHORITY + "/orders");
    public static final Uri CONTENT_URI_ARTICLE_LIKES = Uri.parse("content://" + AUTHORITY + "/article_likes");
    public static final Uri CONTENT_URI_CIRCLE_LIKES = Uri.parse("content://" + AUTHORITY + "/circle_likes");
    public static final Uri CONTENT_URI_CIRCLE_FAVORITES = Uri.parse("content://" + AUTHORITY + "/circle_favorites");
    public static final Uri CONTENT_URI_PRODUCT_FAVORITES = Uri.parse("content://" + AUTHORITY + "/product_favorites");
    public static final Uri CONTENT_URI_PRODUCT_FOOTPRINTS = Uri.parse("content://" + AUTHORITY + "/product_footprints");
    public static final Uri CONTENT_URI_STORE_FOOTPRINTS = Uri.parse("content://" + AUTHORITY + "/store_footprints");
    public static final Uri CONTENT_URI_COMMENTS = Uri.parse("content://" + AUTHORITY + "/comments");
    public static final Uri CONTENT_URI_COMMENT_LIKES = Uri.parse("content://" + AUTHORITY + "/comment_likes");
    public static final Uri CONTENT_URI_FOLLOWS = Uri.parse("content://" + AUTHORITY + "/follows");
    public static final Uri CONTENT_URI_PRODUCT_COMMENTS = Uri.parse("content://" + AUTHORITY + "/product_comments");
    public static final Uri CONTENT_URI_CHAT_MESSAGES = Uri.parse("content://" + AUTHORITY + "/chat_messages");
    public static final Uri CONTENT_URI_PURCHASE_REQUESTS = Uri.parse("content://" + AUTHORITY + "/purchase_requests");
    public static final Uri CONTENT_URI_PURCHASE_QUOTES = Uri.parse("content://" + AUTHORITY + "/purchase_quotes");
    public static final Uri CONTENT_URI_ADDRESSES = Uri.parse("content://" + AUTHORITY + "/addresses");

    // ↓ 每张表对应一个「整数编号」，UriMatcher 用它来快速识别请求的是哪张表
    private static final int USERS = 1;
    private static final int ARTICLES = 2;
    private static final int PRODUCTS = 3;
    private static final int CART = 4;
    private static final int ORDERS = 5;
    private static final int ARTICLE_LIKES = 6;
    private static final int PRODUCT_FAVORITES = 7;
    private static final int PRODUCT_FOOTPRINTS = 8;
    private static final int STORE_FOOTPRINTS = 9;
    private static final int COMMENTS = 10;
    private static final int COMMENT_LIKES = 11;
    private static final int FOLLOWS = 12;
    private static final int PRODUCT_COMMENTS = 13;
    private static final int CHAT_MESSAGES = 14;
    private static final int PURCHASE_REQUESTS = 15;
    private static final int PURCHASE_QUOTES = 16;
    private static final int ADDRESSES = 17;
    private static final int CIRCLE_LIKES = 18;
    private static final int CIRCLE_FAVORITES = 19;

    // 地址识别器：把「Uri 路径」登记成对应的编号
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // static 代码块：类加载时执行一次，把所有「路径→编号」的对应关系登记进去
    static {
        uriMatcher.addURI(AUTHORITY, "users", USERS);
        uriMatcher.addURI(AUTHORITY, "articles", ARTICLES);
        uriMatcher.addURI(AUTHORITY, "products", PRODUCTS);
        uriMatcher.addURI(AUTHORITY, "cart", CART);
        uriMatcher.addURI(AUTHORITY, "orders", ORDERS);
        uriMatcher.addURI(AUTHORITY, "article_likes", ARTICLE_LIKES);
        uriMatcher.addURI(AUTHORITY, "circle_likes", CIRCLE_LIKES);
        uriMatcher.addURI(AUTHORITY, "circle_favorites", CIRCLE_FAVORITES);
        uriMatcher.addURI(AUTHORITY, "product_favorites", PRODUCT_FAVORITES);
        uriMatcher.addURI(AUTHORITY, "product_footprints", PRODUCT_FOOTPRINTS);
        uriMatcher.addURI(AUTHORITY, "store_footprints", STORE_FOOTPRINTS);
        uriMatcher.addURI(AUTHORITY, "comments", COMMENTS);
        uriMatcher.addURI(AUTHORITY, "comment_likes", COMMENT_LIKES);
        uriMatcher.addURI(AUTHORITY, "follows", FOLLOWS);
        uriMatcher.addURI(AUTHORITY, "product_comments", PRODUCT_COMMENTS);
        uriMatcher.addURI(AUTHORITY, "chat_messages", CHAT_MESSAGES);
        uriMatcher.addURI(AUTHORITY, "purchase_requests", PURCHASE_REQUESTS);
        uriMatcher.addURI(AUTHORITY, "purchase_quotes", PURCHASE_QUOTES);
        uriMatcher.addURI(AUTHORITY, "addresses", ADDRESSES);
    }

    private AppDatabase dbHelper;   // 数据库帮手，真正干活的还是它

    // onCreate：Provider 创建时调用，拿到数据库单例备用
    @Override
    public boolean onCreate() {
        dbHelper = AppDatabase.getInstance(getContext());
        return true;
    }

    // 查询：根据 Uri 找到表名，执行 SQL 查询并返回结果游标(Cursor)
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
            @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();   // 读用「可读」库
        Cursor cursor = db.query(tableName(uri), projection, selection, selectionArgs, null, null, sortOrder);
        if (cursor != null && getContext() != null) {
            // 给结果绑定通知地址：当这张表数据变化时，正在展示它的界面能自动收到刷新通知
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    // getType：返回数据的 MIME 类型，本项目用不到，直接返回 null
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    // 插入：往对应表插一条数据，成功后通知界面刷新，并返回新数据的 Uri
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();   // 写用「可写」库
        long id = db.insert(tableName(uri), null, values);
        Uri returnUri = Uri.withAppendedPath(uri, String.valueOf(id));
        if (id > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null); // 广播：数据变了，请刷新
        }
        return returnUri;
    }

    // 删除：删掉符合条件的数据，返回删除条数，成功后通知刷新
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete(tableName(uri), selection, selectionArgs);
        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    // 更新：修改符合条件的数据，返回更新条数，成功后通知刷新
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.update(tableName(uri), values, selection, selectionArgs);
        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    // 把传进来的 Uri 翻译成「数据库表名」。匹配不到就抛异常（防止访问不存在的表）。
    private String tableName(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case USERS:
                return "users";
            case ARTICLES:
                return "articles";
            case PRODUCTS:
                return "products";
            case CART:
                return "cart";
            case ORDERS:
                return "orders";
            case ARTICLE_LIKES:
                return "article_likes";
            case CIRCLE_LIKES:
                return "circle_likes";
            case CIRCLE_FAVORITES:
                return "circle_favorites";
            case PRODUCT_FAVORITES:
                return "product_favorites";
            case PRODUCT_FOOTPRINTS:
                return "product_footprints";
            case STORE_FOOTPRINTS:
                return "store_footprints";
            case COMMENTS:
                return "comments";
            case COMMENT_LIKES:
                return "comment_likes";
            case FOLLOWS:
                return "follows";
            case PRODUCT_COMMENTS:
                return "product_comments";
            case CHAT_MESSAGES:
                return "chat_messages";
            case PURCHASE_REQUESTS:
                return "purchase_requests";
            case PURCHASE_QUOTES:
                return "purchase_quotes";
            case ADDRESSES:
                return "addresses";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
}
