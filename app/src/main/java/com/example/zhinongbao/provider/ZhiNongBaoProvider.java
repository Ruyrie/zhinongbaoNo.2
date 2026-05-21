package com.example.zhinongbao.provider;

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

    public static final String AUTHORITY = "com.example.zhinongbao.provider";
    public static final Uri CONTENT_URI_USERS = Uri.parse("content://" + AUTHORITY + "/users");
    public static final Uri CONTENT_URI_ARTICLES = Uri.parse("content://" + AUTHORITY + "/articles");
    public static final Uri CONTENT_URI_PRODUCTS = Uri.parse("content://" + AUTHORITY + "/products");
    public static final Uri CONTENT_URI_CART = Uri.parse("content://" + AUTHORITY + "/cart");
    public static final Uri CONTENT_URI_ORDERS = Uri.parse("content://" + AUTHORITY + "/orders");
    public static final Uri CONTENT_URI_ARTICLE_LIKES = Uri.parse("content://" + AUTHORITY + "/article_likes");
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

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "users", USERS);
        uriMatcher.addURI(AUTHORITY, "articles", ARTICLES);
        uriMatcher.addURI(AUTHORITY, "products", PRODUCTS);
        uriMatcher.addURI(AUTHORITY, "cart", CART);
        uriMatcher.addURI(AUTHORITY, "orders", ORDERS);
        uriMatcher.addURI(AUTHORITY, "article_likes", ARTICLE_LIKES);
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

    private AppDatabase dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = AppDatabase.getInstance(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
            @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(tableName(uri), projection, selection, selectionArgs, null, null, sortOrder);
        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(tableName(uri), null, values);
        Uri returnUri = Uri.withAppendedPath(uri, String.valueOf(id));
        if (id > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete(tableName(uri), selection, selectionArgs);
        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

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
