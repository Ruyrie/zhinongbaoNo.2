package com.example.zhinongbao.repository;

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
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";

    private final Context context;
    private final ContentResolver resolver;

    public AddressRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    public List<Address> getAddresses(String username) {
        List<Address> addresses = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ADDRESSES,
                new String[] { "id", "receiver_name", "phone", "address", "is_default" },
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
                addresses.add(address);
            }
        }
        return addresses;
    }

    public void saveAddress(String username, long id, String name, String phone, String address, boolean isDefault) {
        if (isDefault) {
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
        if (id > 0) {
            resolver.update(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, values,
                    "id=? AND username=?", new String[] { String.valueOf(id), username });
        } else {
            resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, values);
        }
    }

    public void setDefaultAddress(String username, long id) {
        ContentValues clear = new ContentValues();
        clear.put("is_default", 0);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, clear, "username=?", new String[] { username });
        ContentValues selected = new ContentValues();
        selected.put("is_default", 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES, selected,
                "id=? AND username=?", new String[] { String.valueOf(id), username });
    }

    public void deleteAddress(String username, long id) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_ADDRESSES,
                "id=? AND username=?", new String[] { String.valueOf(id), username });
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }
}
