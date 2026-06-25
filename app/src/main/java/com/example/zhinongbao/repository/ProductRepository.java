package com.example.zhinongbao.repository;

/* ============================================================
 * 【商品 / 商城 / Product】数据仓库（Repository，商品相关一切数据）
 * ============================================================
 * 这个文件是干什么的：商城/商品功能最核心、最大的数据出入口，几乎覆盖整个买卖链路：
 *   - 商品本身：查（单个/全部/按卖家）、加、改、删、生成新 id、浏览量+1。
 *   - 浏览足迹：记录我看过的商品/店铺，并按时间倒序读出来。
 *   - 收藏：判断是否收藏、收藏/取消（toggle）、读我的收藏。
 *   - 购买相关：加购物车、取默认地址、直接下单、是否购买过、有无未完成订单。
 *   - 商品评价：发表、删除、读取评价及统计条数。
 *   - 店铺搜索：按关键字搜店铺/商品并聚合成店铺结果。
 *   - 销量统计：某商品的订单数、销售额。
 *
 * 技术点：
 *   - productProjection + cursorToProduct：统一「要查哪些列」和「把一行转成 Product」。
 *   - 收藏/足迹都用「先删后插」保证唯一、并刷新时间。
 *   - LIKE %关键字% 实现模糊搜索。
 *
 * 谁在用它：商城/商品详情/搜索/收藏/足迹/添加商品/卖家货品 等众多 Presenter。
 * 提示：在 IDE 里搜索「商品」或「商城」可看本组相关文件。
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

    // 取当前登录用户名
    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    // 按 id 查单个商品（查不到返回 null）
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

    // 取全部商品（最新在前）
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

    // 取某卖家发布的全部商品（卖家「我的货品」用）
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

    // 新增商品：卖家发布一件新商品（id 自动取下一个，卖家=当前登录用户）
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

    // 修改商品：编辑已发布商品的信息
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

    // 记录一次商品浏览：商品浏览量+1，并在「商品足迹」里记下我此刻看过它（先删旧记录再插，保证置顶最新）
    public void recordProductView(String username, int productId) {
        Product product = getProductById(productId);
        ContentValues productValues = new ContentValues();
        productValues.put("view_count", product == null ? 1 : product.viewCount + 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_PRODUCTS, productValues,
                "id=?", new String[] { String.valueOf(productId) });
        if (username == null || username.isEmpty()) {
            return;   // 没登录就只加浏览量，不记个人足迹
        }
        ContentValues footprint = new ContentValues();
        footprint.put("username", username);
        footprint.put("product_id", productId);
        footprint.put("viewed_at", System.currentTimeMillis());
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FOOTPRINTS,
                "username=? AND product_id=?", new String[] { username, String.valueOf(productId) });
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_FOOTPRINTS, footprint);
    }

    // 记录一次店铺浏览（进别人店铺时）。不记自己的店；同样先删后插保持最新。
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

    // 取「我浏览过的商品」足迹列表（按浏览时间倒序）
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

    // 取「我浏览过的店铺」足迹列表（按浏览时间倒序），并补全每家店的展示信息
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

    // 取店铺联系电话：优先店铺电话，没有就用账号手机号
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

    // 搜索店铺：先按「用户名/昵称/店名」匹配用户；再把「商品名/描述/分类」命中的商品所属店铺也补进来
    public List<StoreSearchResult> searchStores(String keyword) {
        List<StoreSearchResult> stores = new ArrayList<>();
        String query = keyword == null ? "" : keyword.trim();
        if (query.isEmpty()) {
            return stores;
        }
        String like = "%" + query + "%";
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "username", "store_name", "nickname", "store_phone", "phone", "avatar_uri", "role" },
                "username LIKE ? OR nickname LIKE ? OR store_name LIKE ?",
                new String[] { like, like, like },
                "id ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                StoreSearchResult item = cursorToStoreSearchResult(cursor);
                if (item.productCount > 0 || cursor.getInt(6) == User.ROLE_SELLER || cursor.getInt(6) == User.ROLE_BOTH
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

    // 判断某商品是否已被该用户收藏
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

    // 收藏开关：已收藏则取消（返回 false），未收藏则收藏（返回 true）。返回值=操作后的收藏状态。
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

    // 加入购物车：已在车里则数量+1，否则新增一条数量为 1 的记录
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

    // 取用户默认收货地址（下单时用）
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

    // 直接下单：在商品详情页「立即购买」时，按指定数量生成一条待付款订单
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

    // 删除商品（卖家下架）
    public void deleteProduct(int productId) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PRODUCTS, "id=?", new String[] { String.valueOf(productId) });
    }

    // 统计某商品被下单的次数（销量参考）
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

    // 统计某商品累计销售额：把所有已完成订单的(单价×数量-折扣-退款)累加
    public double getProductSalesRevenue(int productId) {
        double revenue = 0;
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "price", "unit_price", "quantity", "discount", "refund_amount" },
                "product_id=? AND status=?",
                new String[] { String.valueOf(productId), Order.STATUS_COMPLETED },
                null)) {
            while (cursor != null && cursor.moveToNext()) {
                double unit = cursor.getDouble(1) > 0 ? cursor.getDouble(1) : cursor.getDouble(0);   // 改价优先
                revenue += Math.max(0, unit * cursor.getInt(2) - cursor.getDouble(3) - cursor.getDouble(4));
            }
        }
        return revenue;
    }

    // 统计某商品的评价条数
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

    // 判断该用户是否「买过并完成」此商品（用于：只有买过的人才能评价）
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

    /** 该用户对此商品是否存在已付款但尚未确认收货的订单（待发货 / 已发货）。 */
    public boolean hasUnconfirmedOrder(String username, int productId) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "id" },
                "username=? AND product_id=? AND (status=? OR status=?)",
                new String[] { username, String.valueOf(productId),
                        Order.STATUS_PAID, Order.STATUS_SHIPPED },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // 发表商品评价（可带配图）
    public void addProductComment(int productId, String username, String content, String images) {
        ContentValues values = new ContentValues();
        values.put("product_id", productId);
        values.put("username", username);
        values.put("content", content);
        values.put("images", images == null ? "" : images);
        values.put("time", now("MM-dd HH:mm"));
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_COMMENTS, values);
    }

    // 删除某条商品评价
    public void deleteProductComment(int commentId) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PRODUCT_COMMENTS,
                "id=?", new String[] { String.valueOf(commentId) });
    }

    // 取我收藏的全部商品
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

    // 取某商品的全部评价（最新在前），并补全每条评价者的昵称/头像
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

    // 给一条评价补上评价者的昵称和头像（从 users 表查）
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

    // 给一条店铺足迹补全展示信息（店名/电话/头像，缺省时兜底），并统计在售商品数
    private void fillStoreFootprint(StoreFootprint item) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "store_name", "nickname", "store_phone", "phone", "avatar_uri" },
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
                item.avatarUri = cursor.getString(4);
            } else {
                item.storeName = item.seller + "的店铺";
                item.storePhone = "";
                item.avatarUri = "";
            }
        }
        item.productCount = getProductsBySeller(item.seller).size();
    }

    // 把用户查询的一行转成「店铺搜索结果」（店名/电话缺省时兜底，统计在售商品数）
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
        item.avatarUri = cursor.getString(5);
        item.productCount = getProductsBySeller(item.seller).size();
        return item;
    }

    // 根据卖家用户名补全店铺搜索结果（复用 cursorToStoreSearchResult）
    private void fillStoreSearchResult(StoreSearchResult item) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "username", "store_name", "nickname", "store_phone", "phone", "avatar_uri", "role" },
                "username=?",
                new String[] { item.seller },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                StoreSearchResult filled = cursorToStoreSearchResult(cursor);
                item.storeName = filled.storeName;
                item.storePhone = filled.storePhone;
                item.avatarUri = filled.avatarUri;
                item.productCount = filled.productCount;
            }
        }
    }

    // 不分大小写地判断 value 是否包含 query（搜索匹配用）
    private boolean contains(String value, String query) {
        return value != null && query != null && value.toLowerCase().contains(query.toLowerCase());
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    // 按格式返回当前时间字符串
    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    // 查询商品要取的列名（集中定义，配合 cursorToProduct 按列号取值）
    private String[] productProjection() {
        return new String[] { "id", "name", "`desc`", "price", "cover_uri", "category", "seller", "view_count",
                "brand", "origin", "spec", "package_type" };
    }

    // 把查询结果的当前一行翻译成 Product 对象
    private Product cursorToProduct(Cursor cursor) {
        return new Product(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3),
                cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getInt(7),
                cursor.getString(8), cursor.getString(9), cursor.getString(10), cursor.getString(11));
    }

    // 清理参数：null 转空串，并去掉首尾空格
    private String cleanParam(String value) {
        return value == null ? "" : value.trim();
    }

    // 生成下一个商品 id = 当前最大 id + 1（没有商品则从 1 开始）
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
