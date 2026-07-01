package com.example.zhinongbao.repository;

/* ============================================================
 * 【收货地址 / Address】数据仓库（Repository，跟数据库打交道）
 * ============================================================
 *
 * 技术点：
 *   - 通过 ContentResolver 调用 ZhiNongBaoProvider 读写 addresses 表（不直接碰 SQLite）。
 *   - is_default 字段：1=默认地址、0=普通。设默认时先把该用户所有地址清 0，再把选中那条置 1，
 *     保证「同一用户只有一个默认地址」。
 *
 * 谁在用它：AddressPresenter / 地址管理页。
 * 提示：在 IDE 里搜索「地址」可看本组相关文件。
 * ============================================================ */

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.Address;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.util.ArrayList;
import java.util.List;

public class AddressRepository {
    private static final String PREF_SESSION = "pref_session";   // 存登录信息的文件名
    private static final String KEY_LOGGED_USER = "logged_user"; // 当前登录用户名的键

    private final Context context;
    private final ContentResolver resolver;   // 调用 ContentProvider 的入口

    public AddressRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    // 取当前登录用户名
    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    // 读取某用户的全部收货地址（默认地址排最前：is_default DESC）
    public List<Address> getAddresses(String username) {
        List<Address> addresses = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ADDRESSES,
                new String[] { "id", "receiver_name", "phone", "address", "is_default", "tag" },
                "username=?",
                new String[] { username },
                "is_default DESC, id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                Address address = new Address();
                address.id = cursor.getLong(0);
                address.username = username;
                address.receiverName = cursor.getString(1);
                address.phone = cursor.getString(2);
                address.address = cursor.getString(3);
                address.isDefault = cursor.getInt(4) == 1;
                address.tag = cursor.getString(5);
                addresses.add(address);
            }
        }
        return addresses;
    }

    // 保存地址：id>0 表示修改已有地址，否则新增一条。若设为默认，先把其它地址取消默认。
    public void saveAddress(String username, long id, String name, String phone, String address,
            boolean isDefault, String tag) {
        if (isDefault) {
            // 先把该用户所有地址的 is_default 清 0，保证只有一个默认
            ContentValues clear = new ContentValues();
            clear.put("is_default", 0);
            resolver.update(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, clear, "username=?", new String[] { username });
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("receiver_name", name);
        values.put("phone", phone);
        values.put("address", address);
        values.put("is_default", isDefault ? 1 : 0);
        values.put("tag", tag);
        if (id > 0) {
            resolver.update(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, values,
                    "id=? AND username=?", new String[] { String.valueOf(id), username });   // 改
        } else {
            resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, values);                // 增
        }
    }

    // 把指定地址设为默认：先全部清 0，再把这条置 1
    public void setDefaultAddress(String username, long id) {
        ContentValues clear = new ContentValues();
        clear.put("is_default", 0);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, clear, "username=?", new String[] { username });
        ContentValues selected = new ContentValues();
        selected.put("is_default", 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, selected,
                "id=? AND username=?", new String[] { String.valueOf(id), username });
    }

    // 删除指定地址（限定 username，防止误删别人的）
    public void deleteAddress(String username, long id) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES,
                "id=? AND username=?", new String[] { String.valueOf(id), username });
    }

    // 取得存登录信息的 SharedPreferences
    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }
}
