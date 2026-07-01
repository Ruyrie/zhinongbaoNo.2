package com.example.zhinongbao.repository;

/* ============================================================
 * 【采购市场 / Purchase】数据仓库（Repository，采购需求与报价数据）
 * ============================================================
 *
 * 主要能力：
 *   - 需求：发布、修改、删除、查（全部/排除自己/我的）。
 *   - 报价：是否报过价、提交/更新报价、查某需求的全部报价、查我作为卖家提交的报价。
 *   - 买家决策：同意报价(acceptQuote 会下单)、拒绝报价(rejectQuote)。
 *   - 自动维护：采购订单被取消或超 24 小时未付款，把对应「已接受」报价恢复为「待处理」，
 *     好让买家能重新选择（reopenQuotesForInactiveOrders）。
 *
 * 技术点：requestProjection/quoteProjection + cursorToRequest/cursorToQuote 统一取列与转换。
 * 谁在用它：采购市场、发布采购、卖家采购管理 等 Presenter。
 * 提示：在 IDE 里搜索「采购」可看本组相关文件。
 * ============================================================ */

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.PurchaseQuote;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.model.Address;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.User;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.util.ArrayList;
import java.util.List;

public class PurchaseRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";
    private static final String KEY_ACTIVE_ROLE = "active_role";

    private final Context context;
    private final ContentResolver resolver;

    public PurchaseRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    // 取当前登录用户名
    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    // 判断当前是否处于卖家身份（卖家/兼具才算）
    public boolean isSellerMode() {
        int role = prefs().getInt(KEY_ACTIVE_ROLE, User.ROLE_BUYER);
        return role == User.ROLE_SELLER || role == User.ROLE_BOTH;
    }

    // 买家发布一条采购需求（买家=当前登录用户）；images 为逗号分隔的需求图片，可为空
    public boolean addPurchaseRequest(String productName, String category, double quantity,
            String unit, double targetPrice, String description, String images) {
        String buyer = getLoggedUser();
        if (buyer == null) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("buyer_user", buyer);
        values.put("product_name", productName);
        values.put("category", category);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("target_price", targetPrice);
        values.put("description", description);
        values.put("images", images == null ? "" : images);
        values.put("timestamp", System.currentTimeMillis());
        return resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_REQUESTS, values) != null;
    }

    // 修改采购需求（需通过 canModifyPurchaseRequest 校验：是本人且未进入实质交易）；images 为逗号分隔的需求图片
    public boolean updatePurchaseRequest(long requestId, String productName, String category, double quantity,
            String unit, double targetPrice, String description, String images) {
        String buyer = getLoggedUser();
        if (!canModifyPurchaseRequest(requestId, buyer)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("product_name", productName);
        values.put("category", category);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("target_price", targetPrice);
        values.put("description", description);
        values.put("images", images == null ? "" : images);
        values.put("timestamp", System.currentTimeMillis());
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_REQUESTS, values,
                "id=? AND buyer_user=?", new String[] { String.valueOf(requestId), buyer }) > 0;
    }

    // 删除采购需求：连带删掉它收到的所有报价
    public boolean deletePurchaseRequest(long requestId) {
        String buyer = getLoggedUser();
        if (!canModifyPurchaseRequest(requestId, buyer)) {
            return false;
        }
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES,
                "request_id=?", new String[] { String.valueOf(requestId) });
        return resolver.delete(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_REQUESTS,
                "id=? AND buyer_user=?", new String[] { String.valueOf(requestId), buyer }) > 0;
    }

    // 判断需求能否被修改/删除：必须是本人发布的，且它产生的采购订单都还停留在「待付款/已取消」（没真正交易）
    public boolean canModifyPurchaseRequest(long requestId, String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        PurchaseRequest request = getRequestById(requestId);
        if (request == null || request.buyerUser == null || !request.buyerUser.equals(username)) {
            return false;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "status" },
                "purchase_request_id=? AND order_type=?",
                new String[] { String.valueOf(requestId), Order.ORDER_TYPE_PROCUREMENT },
                null)) {
            while (cursor != null && cursor.moveToNext()) {
                String status = cursor.getString(0);
                if (!Order.STATUS_PENDING.equals(status) && !Order.STATUS_CANCELLED.equals(status)) {
                    return false;
                }
            }
        }
        return true;
    }

    // 按 id 取单条采购需求
    public PurchaseRequest getPurchaseRequestById(long requestId) {
        return getRequestById(requestId);
    }

    // 取某采购需求买家上传的需求图片（采购订单展示兜底用：订单自带 proof_images 为空时回源到需求）
    public String getRequestImages(long requestId) {
        if (requestId <= 0) {
            return null;
        }
        PurchaseRequest request = getRequestById(requestId);
        return request == null ? null : request.images;
    }

    // 取某需求下指定卖家报价的配图（采购订单详情兜底用：订单自带的 proof_images 为空时回源到报价）
    public String getQuoteImages(long requestId, String sellerUser) {
        if (requestId <= 0 || sellerUser == null || sellerUser.isEmpty()) {
            return null;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES,
                new String[] { "images" },
                "request_id=? AND seller_user=?",
                new String[] { String.valueOf(requestId), sellerUser },
                "timestamp DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        return null;
    }

    // 取采购市场全部需求（最新在前），并补上买家昵称和已收到的报价数
    public List<PurchaseRequest> getPurchaseRequests() {
        List<PurchaseRequest> list = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_REQUESTS,
                requestProjection(),
                null,
                null,
                "timestamp DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                PurchaseRequest request = cursorToRequest(cursor);
                request.buyerNickname = getNickname(request.buyerUser);
                request.quoteCount = getQuoteCount(request.id);
                list.add(request);
            }
        }
        return list;
    }

    // 卖家视角：采购市场需求列表（去掉卖家自己发布的需求，自己不给自己报价）
    public List<PurchaseRequest> getMarketRequestsForSeller(String seller) {
        List<PurchaseRequest> list = getPurchaseRequests();
        list.removeIf(request -> request.buyerUser != null && request.buyerUser.equals(seller));
        return list;
    }

    // 买家视角：我发布的采购需求
    public List<PurchaseRequest> getMyPurchaseRequests(String username) {
        List<PurchaseRequest> list = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_REQUESTS,
                requestProjection(),
                "buyer_user=?",
                new String[] { username },
                "timestamp DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                PurchaseRequest request = cursorToRequest(cursor);
                request.buyerNickname = getNickname(request.buyerUser);
                request.quoteCount = getQuoteCount(request.id);
                list.add(request);
            }
        }
        return list;
    }

    // 判断该卖家是否已对某需求报过价
    public boolean hasQuoted(long requestId, String sellerUser) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES,
                new String[] { "id" },
                "request_id=? AND seller_user=?",
                new String[] { String.valueOf(requestId), sellerUser },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    // 该采购需求是否已被「有效已付款订单」锁定：买家已付款（待发货/已发货）或已完成且未退款时锁定，
    // 此时卖家不能再（重新）报价；只有订单取消或退款成功后才解锁，可重新报价。
    public boolean isRequestLockedByPaidOrder(long requestId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "status", "refund_amount" },
                "purchase_request_id=? AND order_type=?",
                new String[] { String.valueOf(requestId), Order.ORDER_TYPE_PROCUREMENT },
                null)) {
            while (cursor != null && cursor.moveToNext()) {
                String status = cursor.getString(0);
                double refund = cursor.getDouble(1);
                if (Order.STATUS_PAID.equals(status) || Order.STATUS_SHIPPED.equals(status)) {
                    return true;   // 已付款、待发货/已发货 → 锁定
                }
                if (Order.STATUS_COMPLETED.equals(status) && refund <= 0) {
                    return true;   // 已完成且未退款 → 锁定（退款后 refund>0 解锁）
                }
            }
        }
        return false;
    }

    // 提交报价（不带图）——转调下面的完整版
    public boolean addQuote(long requestId, String sellerUser, double price, String description) {
        return addQuote(requestId, sellerUser, price, description, "");
    }

    // 提交报价（可带图）：已报过则更新，否则新增；状态初始为 pending
    public boolean addQuote(long requestId, String sellerUser, double price, String description, String images) {
        ContentValues values = new ContentValues();
        values.put("request_id", requestId);
        values.put("seller_user", sellerUser);
        values.put("price", price);
        values.put("description", description);
        values.put("images", images == null ? "" : images);
        values.put("timestamp", System.currentTimeMillis());
        values.put("status", "pending");
        boolean ok;
        if (hasQuoted(requestId, sellerUser)) {
            ok = resolver.update(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES, values,
                    "request_id=? AND seller_user=?", new String[] { String.valueOf(requestId), sellerUser }) > 0;
        } else {
            ok = resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES, values) != null;
        }
        if (ok) {
            ensureSellerCapability(sellerUser);
        }
        return ok;
    }

    private void ensureSellerCapability(String sellerUser) {
        if (sellerUser == null || sellerUser.isEmpty()) {
            return;
        }
        UserRepository userRepository = new UserRepository(context);
        if (userRepository.getUserRole(sellerUser) == User.ROLE_BUYER) {
            userRepository.updateUserRole(sellerUser, User.ROLE_BOTH);
        }
    }

    // 取某需求收到的全部报价（按时间正序），并补上报价卖家昵称
    public List<PurchaseQuote> getQuotesForRequest(long requestId) {
        List<PurchaseQuote> list = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES,
                quoteProjection(),
                "request_id=?",
                new String[] { String.valueOf(requestId) },
                "timestamp ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                PurchaseQuote quote = cursorToQuote(cursor);
                quote.sellerNickname = getNickname(quote.sellerUser);
                list.add(quote);
            }
        }
        return list;
    }

    // 取「我作为卖家提交过的所有报价」（卖家采购管理用），并补上对应需求的商品名
    public List<PurchaseQuote> getQuotesBySellerUser(String sellerUser) {
        List<PurchaseQuote> list = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES,
                quoteProjection(),
                "seller_user=?",
                new String[] { sellerUser },
                "timestamp DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                PurchaseQuote quote = cursorToQuote(cursor);
                quote.sellerNickname = getNickname(quote.sellerUser);
                PurchaseRequest request = getRequestById(quote.requestId);
                quote.requestProductName = request == null ? "" : request.productName;
                list.add(quote);
            }
        }
        return list;
    }

    // 取报价（带最新状态）：先跑一遍「失效订单回收」，再返回报价列表
    public List<PurchaseQuote> getQuotesForRequestWithStatus(long requestId) {
        reopenQuotesForInactiveOrders(requestId);
        return getQuotesForRequest(requestId);
    }

    /**
     * 采购订单被取消、或超过 24 小时仍未付款时，将对应的已接受报价恢复为「待处理」，
     * 使买家可以重新同意该报价或其它报价。
     */
    private void reopenQuotesForInactiveOrders(long requestId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "order_id", "seller", "status", "time" },
                "purchase_request_id=? AND order_type=?",
                new String[] { String.valueOf(requestId), Order.ORDER_TYPE_PROCUREMENT },
                null)) {
            while (cursor != null && cursor.moveToNext()) {
                String orderId = cursor.getString(0);
                String seller = cursor.getString(1);
                String status = cursor.getString(2);
                String time = cursor.getString(3);

                if (Order.STATUS_PENDING.equals(status) && isPaymentExpired(time)) {
                    ContentValues ov = new ContentValues();
                    ov.put("status", Order.STATUS_CANCELLED);
                    resolver.update(ZhiNongBaoProvider.CONTENT_URI_ORDERS, ov,
                            "order_id=?", new String[] { orderId });
                    status = Order.STATUS_CANCELLED;
                }

                if (Order.STATUS_CANCELLED.equals(status) && seller != null) {
                    ContentValues qv = new ContentValues();
                    qv.put("status", "pending");
                    qv.putNull("reply_desc");
                    resolver.update(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES, qv,
                            "request_id=? AND seller_user=? AND status=?",
                            new String[] { String.valueOf(requestId), seller, "accepted" });
                }
            }
        }
    }

    // 判断下单时间是否已超过 24 小时未付款
    private boolean isPaymentExpired(String time) {
        if (time == null || time.trim().isEmpty()) {
            return false;
        }
        try {
            java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",
                    java.util.Locale.getDefault()).parse(time);
            if (date == null) {
                return false;
            }
            return System.currentTimeMillis() - date.getTime() >= 24L * 60 * 60 * 1000;
        } catch (java.text.ParseException e) {
            return false;
        }
    }

    /** 同意报价并生成待付款采购订单，返回新订单号；失败返回 null。 */
    // 流程：把该报价标记为 accepted → 取出报价和对应需求 → 用买家默认地址生成一笔采购订单
    public String acceptQuote(long quoteId, String replyDesc) {
        ContentValues values = new ContentValues();
        values.put("status", "accepted");
        values.put("reply_desc", replyDesc);
        boolean ok = resolver.update(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES, values,
                "id=?", new String[] { String.valueOf(quoteId) }) > 0;
        if (!ok) {
            return null;
        }
        PurchaseQuote quote = getQuoteById(quoteId);
        PurchaseRequest request = quote == null ? null : getRequestById(quote.requestId);
        if (quote == null || request == null) {
            return null;
        }
        Address address = getDefaultAddress(request.buyerUser);
        String orderId = "JN" + System.currentTimeMillis();
        ContentValues order = new ContentValues();
        order.put("order_id", orderId);
        order.put("username", request.buyerUser);
        order.put("product_id", 0);
        order.put("name", request.productName);
        order.put("price", quote.price);
        order.put("quantity", (int) request.quantity);
        order.put("time", now("yyyy-MM-dd HH:mm"));
        order.put("status", Order.STATUS_PENDING);
        order.put("order_type", Order.ORDER_TYPE_PROCUREMENT);
        order.put("purchase_request_id", request.id);
        order.put("seller", quote.sellerUser);
        order.put("unit_price", quote.price);
        // 采购订单的展示图统一用「买家发布需求时上传的图片」
        if (request.images != null && !request.images.trim().isEmpty()) {
            order.put("proof_images", request.images);
        }
        if (address != null) {
            order.put("receiver_name", address.receiverName);
            order.put("receiver_phone", address.phone);
            order.put("receiver_address", address.address);
        }
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ORDERS, order);
        return orderId;
    }

    // 拒绝报价：把报价状态改为 rejected，并记下拒绝备注
    public boolean rejectQuote(long quoteId, String replyDesc) {
        ContentValues values = new ContentValues();
        values.put("status", "rejected");
        values.put("reply_desc", replyDesc);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES, values,
                "id=?", new String[] { String.valueOf(quoteId) }) > 0;
    }

    // 判断用户是否有默认收货地址（同意报价下单前需要）
    public boolean hasDefaultAddress(String username) {
        return getDefaultAddress(username) != null;
    }

    // 按 id 查需求，并补上昵称和报价数（内部复用）
    private PurchaseRequest getRequestById(long id) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_REQUESTS,
                requestProjection(),
                "id=?",
                new String[] { String.valueOf(id) },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                PurchaseRequest request = cursorToRequest(cursor);
                request.buyerNickname = getNickname(request.buyerUser);
                request.quoteCount = getQuoteCount(request.id);
                return request;
            }
        }
        return null;
    }

    // 按 id 查报价，并补上卖家昵称
    private PurchaseQuote getQuoteById(long id) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES,
                quoteProjection(),
                "id=?",
                new String[] { String.valueOf(id) },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                PurchaseQuote quote = cursorToQuote(cursor);
                quote.sellerNickname = getNickname(quote.sellerUser);
                return quote;
            }
        }
        return null;
    }

    // 取用户默认收货地址（生成采购订单时填进去）
    private Address getDefaultAddress(String username) {
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

    // 按格式返回当前时间字符串
    private String now(String pattern) {
        return new java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(new java.util.Date());
    }

    // 统计某需求收到的报价条数
    private int getQuoteCount(long requestId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES,
                new String[] { "id" },
                "request_id=?",
                new String[] { String.valueOf(requestId) },
                null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    // 取昵称（为空用用户名顶替）
    private String getNickname(String username) {
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

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    // 查询采购需求要取的列名（配合 cursorToRequest 按列号取值）
    private String[] requestProjection() {
        return new String[] { "id", "buyer_user", "product_name", "category", "quantity", "unit",
                "target_price", "description", "images", "timestamp" };
    }

    // 把查询结果当前一行翻译成 PurchaseRequest 对象
    private PurchaseRequest cursorToRequest(Cursor cursor) {
        PurchaseRequest request = new PurchaseRequest();
        request.id = cursor.getLong(0);
        request.buyerUser = cursor.getString(1);
        request.productName = cursor.getString(2);
        request.category = cursor.getString(3);
        request.quantity = cursor.getDouble(4);
        request.unit = cursor.getString(5);
        request.targetPrice = cursor.getDouble(6);
        request.description = cursor.getString(7);
        request.images = cursor.getString(8);
        request.timestamp = cursor.getLong(9);
        return request;
    }

    // 查询报价要取的列名（配合 cursorToQuote 按列号取值）
    private String[] quoteProjection() {
        return new String[] { "id", "request_id", "seller_user", "price", "description",
                "images", "timestamp", "status", "reply_desc" };
    }

    // 把查询结果当前一行翻译成 PurchaseQuote 对象（状态缺省为 pending）
    private PurchaseQuote cursorToQuote(Cursor cursor) {
        PurchaseQuote quote = new PurchaseQuote();
        quote.id = cursor.getLong(0);
        quote.requestId = cursor.getLong(1);
        quote.sellerUser = cursor.getString(2);
        quote.price = cursor.getDouble(3);
        quote.description = cursor.getString(4);
        quote.images = cursor.getString(5);
        quote.timestamp = cursor.getLong(6);
        quote.status = cursor.getString(7);
        quote.replyDesc = cursor.getString(8);
        if (quote.status == null) {
            quote.status = "pending";
        }
        return quote;
    }
}
