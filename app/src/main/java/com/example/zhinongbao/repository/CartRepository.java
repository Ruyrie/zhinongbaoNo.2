package com.example.zhinongbao.repository;

/* ============================================================
 * 【购物车 / Cart】数据仓库（Repository，专门跟数据库打交道）
 * ============================================================
 * 这个文件是干什么的：
 *   购物车相关的「读/写数据库」全部集中在这里：读取购物车、保存购物车、
 *   查商品、查默认收货地址、把购物车商品变成订单。
 *
 * 关键技术：
 *   - ContentResolver + ZhiNongBaoProvider：本项目规定「业务数据一律走
 *     ContentProvider（数据库统一入口）」，不直接碰 SQLite。resolver 就是
 *     调用 Provider 的遥控器，query=查、insert=插、delete=删。
 *   - Cursor：查询结果像一张表格，moveToNext() 一行行往下读，cursor.getXxx(列号) 取值。
 *   - try(Cursor ...)：用完自动关闭，防止内存泄漏。
 *   - SharedPreferences：手机上的小型「键值存储」，这里用来记住「当前登录用户名」。
 *
 * 谁在用它：CartPresenter。数据流向：Presenter → 本类 → Provider → SQLite。
 * 提示：在 IDE 里搜索「购物车」可看本组全部文件。
 * ============================================================ */

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
    private static final String PREF_SESSION = "pref_session";   // 存登录信息的文件名
    private static final String KEY_LOGGED_USER = "logged_user"; // 「当前登录用户名」对应的键

    private final Context context;
    private final ContentResolver resolver;                       // 调用 ContentProvider 的入口

    public CartRepository(Context context) {
        this.context = context.getApplicationContext();           // 用 ApplicationContext 避免内存泄漏
        this.resolver = this.context.getContentResolver();
    }

    // 取出当前登录的用户名（没登录返回 null）
    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    // 读取某用户的购物车：从 cart 表查出该用户的所有商品，组装成 CartItem 列表
    public List<CartItem> getCart(String username) {
        List<CartItem> items = new ArrayList<>();
        // query 参数：表地址、要查的列、条件、条件的值、排序（id DESC = 越新的越靠前）
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CART,
                new String[] { "product_id", "name", "price", "quantity" },
                "username=?",
                new String[] { username },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {       // 一行行往下读
                int productId = cursor.getInt(0);                 // 第0列：商品id
                Product product = getProductById(productId);      // 顺便查商品拿封面图
                String coverUri = product == null ? "" : product.coverUri;
                items.add(new CartItem(productId, cursor.getString(1), cursor.getDouble(2), cursor.getInt(3), coverUri));
            }
        }
        return items;
    }

    // 保存购物车：先删掉该用户旧的全部购物车记录，再把当前列表逐条插入（整体覆盖式保存）
    public void saveCart(String username, List<CartItem> items) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CART, "username=?", new String[] { username });
        for (CartItem item : items) {
            ContentValues values = new ContentValues();           // ContentValues = 「列名→值」的容器
            values.put("username", username);
            values.put("product_id", item.productId);
            values.put("name", item.name);
            values.put("price", item.price);
            values.put("quantity", item.quantity);
            resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CART, values);
        }
    }

    // 按 id 查单个商品（结算校验、取封面图时用）。查不到返回 null。
    public Product getProductById(int productId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCTS,
                new String[] { "id", "name", "`desc`", "price", "cover_uri", "category", "seller", "view_count" },
                "id=?",
                new String[] { String.valueOf(productId) },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {         // 只取第一条
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

    // 查某用户的「默认收货地址」（is_default=1）。结算时必须有，否则不能下单。
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
                address.isDefault = cursor.getInt(4) == 1;        // 数据库存 1/0，转成 true/false
                return address;
            }
        }
        return null;
    }

    // 把购物车里的一件商品变成一条订单，写入 orders 表
    public void addOrder(String username, CartItem item) {
        Product product = getProductById(item.productId);
        // 卖家：取商品的 seller；若为空则记为 "admin"（兜底，避免空卖家）
        String seller = product != null && product.seller != null && !product.seller.trim().isEmpty()
                ? product.seller
                : "admin";
        Address address = getDefaultAddress(username);

        ContentValues values = new ContentValues();
        values.put("order_id", "JN" + System.currentTimeMillis()); // 订单号 = JN+当前毫秒时间，保证唯一
        values.put("username", username);
        values.put("product_id", item.productId);
        values.put("name", item.name);
        values.put("price", item.price);
        values.put("quantity", item.quantity);
        values.put("time", now("yyyy-MM-dd HH:mm"));              // 下单时间
        values.put("status", Order.STATUS_PENDING);              // 初始状态：待付款
        values.put("seller", seller);
        values.put("order_type", Order.ORDER_TYPE_RETAIL);       // 订单类型：零售（区别于采购）
        if (address != null) {                                   // 把收货信息一并写入订单
            values.put("receiver_name", address.receiverName);
            values.put("receiver_phone", address.phone);
            values.put("receiver_address", address.address);
        }
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ORDERS, values);
    }

    // 取得 SharedPreferences（存登录信息的小仓库）
    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    // 按指定格式返回「当前时间」字符串，如 2026-06-24 15:30
    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }
}
