package com.example.zhinongbao.repository;

/* ============================================================
 * 【用户 / 账号 / User】数据仓库（Repository，账号相关一切数据）
 * ============================================================
 *
 * 技术点：
 *   - 两类存储：① 用户资料存数据库 users 表（走 ContentProvider）；
 *     ② 登录态/当前身份等「会话信息」存 SharedPreferences（手机本地键值存储）。
 *   - role 常量见 User 模型：ROLE_BUYER/ROLE_SELLER/ROLE_BOTH。
 *   - 很多方法是「查一个字段返回」的固定套路：query → moveToFirst → getXxx。
 *
 * 谁在用它：登录/注册/改密/资料编辑/账号管理/卖家中心等几乎所有账号功能的 Presenter。
 * 提示：在 IDE 里搜索「用户」或「登录」可看本组相关文件。
 * ============================================================ */

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
    private static final String PREF_SESSION = "pref_session";          // 会话信息文件名
    private static final String KEY_LOGGED_USER = "logged_user";        // 当前登录用户名的键
    private static final String KEY_ACTIVE_ROLE = "active_role";        // 当前使用身份的键（全局）
    private static final String KEY_ACTIVE_ROLE_PREFIX = "active_role_";// 每个用户各自身份键的前缀

    private final Context context;
    private final ContentResolver resolver;

    public UserRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    // 读取当前登录用户名（没登录返回 null）
    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    // 记住当前登录用户名（登录成功时调用）
    public void setLoggedUser(String username) {
        prefs().edit().putString(KEY_LOGGED_USER, username).apply();
    }

    // 设置「当前以哪个身份在用」（买家/卖家），同时按用户名单独记一份
    public void setActiveRole(int role) {
        String username = getLoggedUser();
        SharedPreferences.Editor editor = prefs().edit().putInt(KEY_ACTIVE_ROLE, role);
        if (username != null && !username.isEmpty()) {
            editor.putInt(activeRoleKey(username), role);
        }
        editor.apply();
    }

    // 读取「当前使用身份」。若记录是卖家但该账号其实没有卖家权限，则降级为买家（防越权）。
    public int getActiveRole() {
        String username = getLoggedUser();
        int userRole = getUserRole(username);
        int fallback = userRole == User.ROLE_SELLER ? User.ROLE_SELLER : User.ROLE_BUYER;  // 默认值
        int activeRole = username == null || username.isEmpty()
                ? prefs().getInt(KEY_ACTIVE_ROLE, fallback)
                : prefs().getInt(activeRoleKey(username), fallback);
        if (activeRole == User.ROLE_SELLER && !canUseSellerRole(username)) {
            return User.ROLE_BUYER;
        }
        return activeRole;
    }

    // 判断某用户能否使用卖家身份（角色为卖家或买卖兼具才行）
    public boolean canUseSellerRole(String username) {
        int role = getUserRole(username);
        return role == User.ROLE_SELLER || role == User.ROLE_BOTH;
    }

    // 退出登录：清掉登录用户和当前身份记录
    public void logout() {
        prefs().edit().remove(KEY_LOGGED_USER).remove(KEY_ACTIVE_ROLE).apply();
    }

    // 读取所有用户（聊天选人等用），跳过被本地隐藏的用户
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

    // 把某用户加入「隐藏名单」（本地记录，列表里不再显示）
    public void hideUserFromHistory(String username) {
        Set<String> hidden = new HashSet<>(prefs().getStringSet("hidden_users", new HashSet<>()));
        hidden.add(username);
        prefs().edit().putStringSet("hidden_users", hidden).apply();
    }

    // 用「用户名或手机号」找到对应的用户名（忘记密码时定位账号用）
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

    // 登录校验：用「用户名或手机号 + 密码」去数据库查，匹配则返回 User，否则返回 null
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

    // 判断某手机号是否已被注册绑定
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

    // 注册新用户：手机号若已被绑定则失败；否则插入一条新用户记录（昵称默认=用户名）
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
        return resolver.insert(ZhiNongBaoProvider.CONTENT_URI_USERS, values) != null;  // insert 成功返回非 null
    }

    // 判断新密码是否和旧密码相同（改密码时拦截「没改」的情况）
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

    // 修改密码：更新成功（影响行数>0）返回 true
    public boolean changePassword(String username, String newPassword) {
        ContentValues values = new ContentValues();
        values.put("password", newPassword);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    // 读取用户绑定的手机号（没有则返回空串）
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

    // 修改绑定手机号：若该号已被别人绑定则失败
    public boolean updatePhone(String username, String phone) {
        if (isPhoneBound(phone)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("phone", phone);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    // 读取昵称（为空则用用户名顶替）
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

    // 修改昵称
    public boolean setNickname(String username, String nickname) {
        ContentValues values = new ContentValues();
        values.put("nickname", nickname);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    // 读取个性签名（为空则给一句默认文案）
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

    // 修改个性签名
    public boolean updateSignature(String username, String signature) {
        ContentValues values = new ContentValues();
        values.put("signature", signature);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    // 读取头像地址
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

    // 修改头像地址
    public boolean setAvatarUri(String username, String uri) {
        ContentValues values = new ContentValues();
        values.put("avatar_uri", uri);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    // 读取用户角色：admin 固定为「买卖兼具」，查不到默认买家
    public int getUserRole(String username) {
        if (username == null) {
            return User.ROLE_BUYER;
        }
        if ("admin".equals(username)) {
            return User.ROLE_BOTH;
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

    // 修改用户角色（如买家升级为卖家）
    public boolean updateUserRole(String username, int role) {
        ContentValues values = new ContentValues();
        values.put("role", role);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    // 读取店铺名：没设置就用「昵称(或用户名)+的店铺」兜底
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

    // 修改店铺信息（店名 + 店铺电话）
    public boolean updateStoreInfo(String username, String storeName, String storePhone) {
        ContentValues values = new ContentValues();
        values.put("store_name", storeName);
        values.put("store_phone", storePhone);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_USERS, values,
                "username=?", new String[] { username }) > 0;
    }

    // 取得存会话信息的 SharedPreferences
    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    // 拼出某用户专属的「当前身份」存储键，如 active_role_admin
    private String activeRoleKey(String username) {
        return KEY_ACTIVE_ROLE_PREFIX + username;
    }
}
