package com.example.zhinongbao.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.Address;
import com.example.zhinongbao.model.CartItem;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CartRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";

    private final Context context;
    private final ContentResolver resolver;

    public CartRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    public List<CartItem> getCart(String username) {
        List<CartItem> items = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CART,
                new String[] { "product_id", "name", "price", "quantity" },
                "username=?",
                new String[] { username },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                int productId = cursor.getInt(0);
                Product product = getProductById(productId);
                String coverUri = product == null ? "" : product.coverUri;
                items.add(new CartItem(productId, cursor.getString(1), cursor.getDouble(2), cursor.getInt(3), coverUri));
            }
        }
        return items;
    }

    public void saveCart(String username, List<CartItem> items) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CART, "username=?", new String[] { username });
        for (CartItem item : items) {
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("product_id", item.productId);
            values.put("name", item.name);
            values.put("price", item.price);
            values.put("quantity", item.quantity);
            resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CART, values);
        }
    }

    public Product getProductById(int productId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCTS,
                new String[] { "id", "name", "`desc`", "price", "cover_uri", "category", "seller", "view_count" },
                "id=?",
                new String[] { String.valueOf(productId) },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new Product(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getDouble(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6),
                        cursor.getInt(7));
            }
        }
        return null;
    }

    public Address getDefaultAddress(String username) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ADDRESSES,
                new String[] { "id", "receiver_name", "phone", "address", "is_default" },
                "username=? AND is_default=1",
                new String[] { username },
            "id DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                Address address = new Address();
                address.id = cursor.getLong(0);
                address.username = username;
                address.receiverName = cursor.getString(1);
                address.phone = cursor.getString(2);
                address.address = cursor.getString(3);
                address.isDefault = cursor.getInt(4) == 1;
                return address;
            }
        }
        return null;
    }

    public void addOrder(String username, CartItem item) {
        Product product = getProductById(item.productId);
        String seller = product != null && product.seller != null && !product.seller.trim().isEmpty()
                ? product.seller
                : "admin";
        Address address = getDefaultAddress(username);

        ContentValues values = new ContentValues();
        values.put("order_id", "JN" + System.currentTimeMillis());
        values.put("username", username);
        values.put("product_id", item.productId);
        values.put("name", item.name);
        values.put("price", item.price);
        values.put("quantity", item.quantity);
        values.put("time", now("yyyy-MM-dd HH:mm"));
        values.put("status", Order.STATUS_PENDING);
        values.put("seller", seller);
        values.put("order_type", Order.ORDER_TYPE_RETAIL);
        if (address != null) {
            values.put("receiver_name", address.receiverName);
            values.put("receiver_phone", address.phone);
            values.put("receiver_address", address.address);
        }
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ORDERS, values);
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }
}
