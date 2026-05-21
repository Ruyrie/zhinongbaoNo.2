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
import com.example.zhinongbao.model.ProductComment;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";

    private final Context context;
    private final ContentResolver resolver;

    public ProductRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    public Product getProductById(int productId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCTS,
                productProjection(),
                "id=?",
                new String[] { String.valueOf(productId) },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToProduct(cursor);
            }
        }
        return null;
    }

    public void recordProductView(String username, int productId) {
        Product product = getProductById(productId);
        ContentValues productValues = new ContentValues();
        productValues.put("view_count", product == null ? 1 : product.viewCount + 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_PRODUCTS, productValues,
                "id=?", new String[] { String.valueOf(productId) });
        if (username == null || username.isEmpty()) {
            return;
        }
        ContentValues footprint = new ContentValues();
        footprint.put("username", username);
        footprint.put("product_id", productId);
        footprint.put("viewed_at", System.currentTimeMillis());
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FOOTPRINTS,
                "username=? AND product_id=?", new String[] { username, String.valueOf(productId) });
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FOOTPRINTS, footprint);
    }

    public String getStorePhone(String seller) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "store_phone", "phone" },
                "username=?",
                new String[] { seller },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String storePhone = cursor.getString(0);
                return storePhone == null || storePhone.trim().isEmpty() ? cursor.getString(1) : storePhone;
            }
        }
        return null;
    }

    public boolean isProductFavorited(String username, int productId) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FAVORITES,
                new String[] { "id" },
                "username=? AND product_id=?",
                new String[] { username, String.valueOf(productId) },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    public boolean toggleProductFavorite(String username, int productId) {
        if (isProductFavorited(username, productId)) {
            resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FAVORITES,
                    "username=? AND product_id=?", new String[] { username, String.valueOf(productId) });
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("product_id", productId);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FAVORITES, values);
        return true;
    }

    public void addToCart(String username, Product product) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CART,
                new String[] { "quantity" },
                "username=? AND product_id=?",
                new String[] { username, String.valueOf(product.id) },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put("quantity", cursor.getInt(0) + 1);
                resolver.update(ZhiNongBaoProvider.CONTENT_URI_CART, values,
                        "username=? AND product_id=?", new String[] { username, String.valueOf(product.id) });
                return;
            }
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("product_id", product.id);
        values.put("name", product.name);
        values.put("price", product.price);
        values.put("quantity", 1);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CART, values);
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

    public void addOrder(String username, Product product, int quantity) {
        Address address = getDefaultAddress(username);
        ContentValues values = new ContentValues();
        values.put("order_id", "JN" + System.currentTimeMillis());
        values.put("username", username);
        values.put("product_id", product.id);
        values.put("name", product.name);
        values.put("price", product.price);
        values.put("quantity", quantity);
        values.put("time", now("yyyy-MM-dd HH:mm"));
        values.put("status", Order.STATUS_PENDING);
        values.put("seller", product.seller == null || product.seller.trim().isEmpty() ? "admin" : product.seller);
        values.put("order_type", Order.ORDER_TYPE_RETAIL);
        if (address != null) {
            values.put("receiver_name", address.receiverName);
            values.put("receiver_phone", address.phone);
            values.put("receiver_address", address.address);
        }
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ORDERS, values);
    }

    public void deleteProduct(int productId) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PRODUCTS, "id=?", new String[] { String.valueOf(productId) });
    }

    public int getProductCommentCount(int productId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCT_COMMENTS,
                new String[] { "id" },
                "product_id=?",
                new String[] { String.valueOf(productId) },
                null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    public boolean hasPurchasedProduct(String username, int productId) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "id" },
                "username=? AND product_id=? AND status=? AND refund_amount=0",
                new String[] { username, String.valueOf(productId), Order.STATUS_COMPLETED },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    public void addProductComment(int productId, String username, String content, String images) {
        ContentValues values = new ContentValues();
        values.put("product_id", productId);
        values.put("username", username);
        values.put("content", content);
        values.put("images", images == null ? "" : images);
        values.put("time", now("MM-dd HH:mm"));
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_COMMENTS, values);
    }

    public void deleteProductComment(int commentId) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_COMMENTS,
                "id=?", new String[] { String.valueOf(commentId) });
    }

    public java.util.List<Product> getFavoriteProducts(String username) {
        java.util.List<Product> products = new java.util.ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FAVORITES,
                new String[] { "product_id" },
                "username=?",
                new String[] { username },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                Product product = getProductById(cursor.getInt(0));
                if (product != null) {
                    products.add(product);
                }
            }
        }
        return products;
    }

    public List<ProductComment> getProductComments(int productId) {
        List<ProductComment> comments = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCT_COMMENTS,
                new String[] { "id", "product_id", "username", "content", "images", "time" },
                "product_id=?",
                new String[] { String.valueOf(productId) },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                ProductComment comment = new ProductComment(cursor.getInt(0), cursor.getInt(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5));
                fillUserInfo(comment);
                comments.add(comment);
            }
        }
        return comments;
    }

    private void fillUserInfo(ProductComment comment) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "nickname", "avatar_uri" },
                "username=?",
                new String[] { comment.username },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                comment.nickname = cursor.getString(0);
                comment.avatarUri = cursor.getString(1);
            }
        }
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    private String[] productProjection() {
        return new String[] { "id", "name", "`desc`", "price", "cover_uri", "category", "seller", "view_count" };
    }

    private Product cursorToProduct(Cursor cursor) {
        return new Product(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3),
                cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getInt(7));
    }
}
