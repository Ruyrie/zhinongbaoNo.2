package com.example.zhinongbao.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.User;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

public class UserRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";
    private static final String KEY_ACTIVE_ROLE = "active_role";

    private final Context context;
    private final ContentResolver resolver;

    public UserRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    public void setLoggedUser(String username) {
        prefs().edit().putString(KEY_LOGGED_USER, username).apply();
    }

    public void setActiveRole(int role) {
        prefs().edit().putInt(KEY_ACTIVE_ROLE, role).apply();
    }

    public void logout() {
        prefs().edit().remove(KEY_LOGGED_USER).remove(KEY_ACTIVE_ROLE).apply();
    }

    public String findUsernameByAccount(String account) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "username" },
                "username=? OR phone=?",
                new String[] { account, account },
                null)) {
            return cursor != null && cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    public User login(String usernameOrPhone, String password) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "username", "password", "role" },
                "(username=? OR phone=?) AND password=?",
                new String[] { usernameOrPhone, usernameOrPhone, password },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                User user = new User(cursor.getString(0), cursor.getString(1));
                user.role = cursor.getInt(2);
                return user;
            }
        }
        return null;
    }

    public boolean isPhoneBound(String phone) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "username" },
                "phone=?",
                new String[] { phone },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    public boolean register(String username, String password, String phone, int role) {
        if (phone != null && !phone.isEmpty() && isPhoneBound(phone)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        values.put("nickname", username);
        values.put("role", role);
        if (phone != null && !phone.isEmpty()) {
            values.put("phone", phone);
        }
        return resolver.insert(ZhiNongBaoProvider.CONTENT_URI_USERS, values) != null;
    }

    public boolean isSameAsOldPassword(String username, String password) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "password" },
                "username=?",
                new String[] { username },
                null)) {
            return cursor != null && cursor.moveToFirst() && password.equals(cursor.getString(0));
        }
    }

    public boolean changePassword(String username, String newPassword) {
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }
}
