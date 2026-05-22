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
import com.example.zhinongbao.model.StoreFootprint;
import com.example.zhinongbao.model.StoreSearchResult;
import com.example.zhinongbao.model.User;
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

    public List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCTS,
                productProjection(),
                null,
                null,
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                products.add(cursorToProduct(cursor));
            }
        }
        return products;
    }

    public List<Product> getProductsBySeller(String seller) {
        List<Product> products = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCTS,
                productProjection(),
                "seller=?",
                new String[] { seller },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                products.add(cursorToProduct(cursor));
            }
        }
        return products;
    }

    public void addProduct(String name, String desc, double price, String coverUri, String category,
            String brand, String origin, String spec, String packageType) {
        ContentValues values = new ContentValues();
        values.put("id", nextProductId());
        values.put("name", name);
        values.put("desc", desc);
        values.put("price", price);
        values.put("cover_uri", coverUri == null ? "" : coverUri);
        values.put("category", category == null || category.isEmpty() ? "推荐" : category);
        values.put("brand", cleanParam(brand));
        values.put("origin", cleanParam(origin));
        values.put("spec", cleanParam(spec));
        values.put("package_type", cleanParam(packageType));
        values.put("view_count", 0);
        values.put("seller", getLoggedUser());
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PRODUCTS, values);
    }

    public boolean updateProduct(int productId, String name, String desc, double price, String coverUri, String category,
            String brand, String origin, String spec, String packageType) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("desc", desc);
        values.put("price", price);
        values.put("cover_uri", coverUri == null ? "" : coverUri);
        values.put("category", category == null || category.isEmpty() ? "推荐" : category);
        values.put("brand", cleanParam(brand));
        values.put("origin", cleanParam(origin));
        values.put("spec", cleanParam(spec));
        values.put("package_type", cleanParam(packageType));
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_PRODUCTS, values,
                "id=?", new String[] { String.valueOf(productId) }) > 0;
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

    public void recordStoreView(String username, String seller) {
        if (username == null || username.isEmpty() || seller == null || seller.isEmpty()
                || username.equals(seller)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("seller", seller);
        values.put("viewed_at", System.currentTimeMillis());
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_STORE_FOOTPRINTS,
                "username=? AND seller=?", new String[] { username, seller });
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_STORE_FOOTPRINTS, values);
    }

    public List<Product> getProductFootprints(String username) {
        List<Product> products = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return products;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FOOTPRINTS,
                new String[] { "product_id", "viewed_at" },
                "username=?",
                new String[] { username },
                "viewed_at DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                Product product = getProductById(cursor.getInt(0));
                if (product != null) {
                    product.viewedAt = cursor.getLong(1);
                    products.add(product);
                }
            }
        }
        return products;
    }

    public List<StoreFootprint> getStoreFootprints(String username) {
        List<StoreFootprint> stores = new ArrayList<>();
        if (username == null || username.isEmpty()) {
            return stores;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_STORE_FOOTPRINTS,
                new String[] { "seller", "viewed_at" },
                "username=?",
                new String[] { username },
                "viewed_at DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                StoreFootprint item = new StoreFootprint();
                item.seller = cursor.getString(0);
                item.viewedAt = cursor.getLong(1);
                fillStoreFootprint(item);
                stores.add(item);
            }
        }
        return stores;
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

    public List<StoreSearchResult> searchStores(String keyword) {
        List<StoreSearchResult> stores = new ArrayList<>();
        String query = keyword == null ? "" : keyword.trim();
        if (query.isEmpty()) {
            return stores;
        }
        String like = "%" + query + "%";
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "username", "store_name", "nickname", "store_phone", "phone", "role" },
                "username LIKE ? OR nickname LIKE ? OR store_name LIKE ?",
                new String[] { like, like, like },
                "id ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                StoreSearchResult item = cursorToStoreSearchResult(cursor);
                if (item.productCount > 0 || cursor.getInt(5) == User.ROLE_SELLER || cursor.getInt(5) == User.ROLE_BOTH
                        || (item.storeName != null && !item.storeName.isEmpty())) {
                    stores.add(item);
                }
            }
        }
        for (Product product : getProducts()) {
            if (contains(product.name, query) || contains(product.desc, query) || contains(product.category, query)) {
                boolean exists = false;
                for (StoreSearchResult store : stores) {
                    if (store.seller != null && store.seller.equals(product.seller)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists && product.seller != null && !product.seller.isEmpty()) {
                    StoreSearchResult item = new StoreSearchResult();
                    item.seller = product.seller;
                    fillStoreSearchResult(item);
                    stores.add(item);
                }
            }
        }
        return stores;
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

    public int getProductOrderCount(int productId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "id" },
                "product_id=?",
                new String[] { String.valueOf(productId) },
                null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    public double getProductSalesRevenue(int productId) {
        double revenue = 0;
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "price", "unit_price", "quantity", "discount", "refund_amount" },
                "product_id=? AND status=?",
                new String[] { String.valueOf(productId), Order.STATUS_COMPLETED },
                null)) {
            while (cursor != null && cursor.moveToNext()) {
                double unit = cursor.getDouble(1) > 0 ? cursor.getDouble(1) : cursor.getDouble(0);
                revenue += Math.max(0, unit * cursor.getInt(2) - cursor.getDouble(3) - cursor.getDouble(4));
            }
        }
        return revenue;
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

    private void fillStoreFootprint(StoreFootprint item) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "store_name", "nickname", "store_phone", "phone" },
                "username=?",
                new String[] { item.seller },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String storeName = cursor.getString(0);
                String nickname = cursor.getString(1);
                item.storeName = storeName != null && !storeName.isEmpty()
                        ? storeName
                        : ((nickname != null && !nickname.isEmpty() ? nickname : item.seller) + "的店铺");
                String storePhone = cursor.getString(2);
                String phone = cursor.getString(3);
                item.storePhone = storePhone != null && !storePhone.isEmpty() ? storePhone : (phone == null ? "" : phone);
            } else {
                item.storeName = item.seller + "的店铺";
                item.storePhone = "";
            }
        }
        item.productCount = getProductsBySeller(item.seller).size();
    }

    private StoreSearchResult cursorToStoreSearchResult(Cursor cursor) {
        StoreSearchResult item = new StoreSearchResult();
        item.seller = cursor.getString(0);
        String storeName = cursor.getString(1);
        String nickname = cursor.getString(2);
        item.storeName = storeName != null && !storeName.trim().isEmpty()
                ? storeName
                : ((nickname != null && !nickname.isEmpty() ? nickname : item.seller) + "的店铺");
        String storePhone = cursor.getString(3);
        String phone = cursor.getString(4);
        item.storePhone = storePhone != null && !storePhone.trim().isEmpty() ? storePhone : (phone == null ? "" : phone);
        item.productCount = getProductsBySeller(item.seller).size();
        return item;
    }

    private void fillStoreSearchResult(StoreSearchResult item) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "username", "store_name", "nickname", "store_phone", "phone", "role" },
                "username=?",
                new String[] { item.seller },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                StoreSearchResult filled = cursorToStoreSearchResult(cursor);
                item.storeName = filled.storeName;
                item.storePhone = filled.storePhone;
                item.productCount = filled.productCount;
            }
        }
    }

    private boolean contains(String value, String query) {
        return value != null && query != null && value.toLowerCase().contains(query.toLowerCase());
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    private String[] productProjection() {
        return new String[] { "id", "name", "`desc`", "price", "cover_uri", "category", "seller", "view_count",
                "brand", "origin", "spec", "package_type" };
    }

    private Product cursorToProduct(Cursor cursor) {
        return new Product(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3),
                cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getInt(7),
                cursor.getString(8), cursor.getString(9), cursor.getString(10), cursor.getString(11));
    }

    private String cleanParam(String value) {
        return value == null ? "" : value.trim();
    }

    private int nextProductId() {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCTS,
                new String[] { "id" },
                null,
                null,
                "id DESC")) {
            return cursor != null && cursor.moveToFirst() ? cursor.getInt(0) + 1 : 1;
        }
    }
}
