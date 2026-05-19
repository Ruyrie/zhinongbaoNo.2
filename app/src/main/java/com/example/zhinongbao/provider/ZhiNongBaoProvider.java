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

    private static final int USERS = 1;
    private static final int ARTICLES = 2;
    private static final int PRODUCTS = 3;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "users", USERS);
        uriMatcher.addURI(AUTHORITY, "articles", ARTICLES);
        uriMatcher.addURI(AUTHORITY, "products", PRODUCTS);
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
        Cursor cursor = null;
        switch (uriMatcher.match(uri)) {
            case USERS:
                cursor = db.query("users", projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case ARTICLES:
                cursor = db.query("articles", projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PRODUCTS:
                cursor = db.query("products", projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
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
        long id = -1;
        Uri returnUri = null;
        switch (uriMatcher.match(uri)) {
            case USERS:
                id = db.insert("users", null, values);
                returnUri = Uri.withAppendedPath(CONTENT_URI_USERS, String.valueOf(id));
                break;
            case ARTICLES:
                id = db.insert("articles", null, values);
                returnUri = Uri.withAppendedPath(CONTENT_URI_ARTICLES, String.valueOf(id));
                break;
            case PRODUCTS:
                id = db.insert("products", null, values);
                returnUri = Uri.withAppendedPath(CONTENT_URI_PRODUCTS, String.valueOf(id));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (id > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case USERS:
                count = db.delete("users", selection, selectionArgs);
                break;
            case ARTICLES:
                count = db.delete("articles", selection, selectionArgs);
                break;
            case PRODUCTS:
                count = db.delete("products", selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case USERS:
                count = db.update("users", values, selection, selectionArgs);
                break;
            case ARTICLES:
                count = db.update("articles", values, selection, selectionArgs);
                break;
            case PRODUCTS:
                count = db.update("products", values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
}
