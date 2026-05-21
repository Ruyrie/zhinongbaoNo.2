package com.example.zhinongbao.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.User;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public int getActiveRole() {
        return prefs().getInt(KEY_ACTIVE_ROLE, User.ROLE_BUYER);
    }

    public void logout() {
        prefs().edit().remove(KEY_LOGGED_USER).remove(KEY_ACTIVE_ROLE).apply();
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        Set<String> hidden = prefs().getStringSet("hidden_users", new HashSet<>());
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "username", "password", "avatar_uri", "role" },
                null,
                null,
                null)) {
            while (cursor != null && cursor.moveToNext()) {
                String username = cursor.getString(0);
                if (hidden.contains(username)) {
                    continue;
                }
                User user = new User(username, cursor.getString(1));
                user.avatarUri = cursor.getString(2);
                user.role = cursor.getInt(3);
                users.add(user);
            }
        }
        return users;
    }

    public void hideUserFromHistory(String username) {
        Set<String> hidden = new HashSet<>(prefs().getStringSet("hidden_users", new HashSet<>()));
        hidden.add(username);
        prefs().edit().putStringSet("hidden_users", hidden).apply();
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

    public String getPhone(String username) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "phone" },
                "username=?",
                new String[] { username },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String phone = cursor.getString(0);
                return phone == null ? "" : phone;
            }
        }
        return "";
    }

    public boolean updatePhone(String username, String phone) {
        if (isPhoneBound(phone)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("phone", phone);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    public String getNickname(String username) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "nickname" },
                "username=?",
                new String[] { username },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String nickname = cursor.getString(0);
                return nickname == null || nickname.isEmpty() ? username : nickname;
            }
        }
        return username;
    }

    public boolean setNickname(String username, String nickname) {
        ContentValues values = new ContentValues();
        values.put("nickname", nickname);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    public String getSignature(String username) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "signature" },
                "username=?",
                new String[] { username },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String signature = cursor.getString(0);
                return signature == null ? "这个人很懒，什么都没留下" : signature;
            }
        }
        return "这个人很懒，什么都没留下";
    }

    public boolean updateSignature(String username, String signature) {
        ContentValues values = new ContentValues();
        values.put("signature", signature);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
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

    public boolean setAvatarUri(String username, String uri) {
        ContentValues values = new ContentValues();
        values.put("avatar_uri", uri);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    public int getUserRole(String username) {
        if (username == null) {
            return User.ROLE_BUYER;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "role" },
                "username=?",
                new String[] { username },
                null)) {
            return cursor != null && cursor.moveToFirst() ? cursor.getInt(0) : User.ROLE_BUYER;
        }
    }

    public boolean updateUserRole(String username, int role) {
        ContentValues values = new ContentValues();
        values.put("role", role);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    public String getStoreName(String username) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "store_name", "nickname" },
                "username=?",
                new String[] { username },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String storeName = cursor.getString(0);
                if (storeName != null && !storeName.trim().isEmpty()) {
                    return storeName;
                }
                String nickname = cursor.getString(1);
                return ((nickname != null && !nickname.isEmpty()) ? nickname : username) + "的店铺";
            }
        }
        return "店铺";
    }

    public boolean updateStoreInfo(String username, String storeName, String storePhone) {
        ContentValues values = new ContentValues();
        values.put("store_name", storeName);
        values.put("store_phone", storePhone);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }
}
