package com.example.zhinongbao.repository;

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

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    public boolean isSellerMode() {
        int role = prefs().getInt(KEY_ACTIVE_ROLE, User.ROLE_BUYER);
        return role == User.ROLE_SELLER || role == User.ROLE_BOTH;
    }

    public boolean addPurchaseRequest(String productName, String category, double quantity,
            String unit, double targetPrice, String description) {
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
        values.put("timestamp", System.currentTimeMillis());
        return resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_REQUESTS, values) != null;
    }

    public boolean updatePurchaseRequest(long requestId, String productName, String category, double quantity,
            String unit, double targetPrice, String description) {
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
        values.put("timestamp", System.currentTimeMillis());
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_REQUESTS, values,
                "id=? AND buyer_user=?", new String[] { String.valueOf(requestId), buyer }) > 0;
    }

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

    public PurchaseRequest getPurchaseRequestById(long requestId) {
        return getRequestById(requestId);
    }

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

    public List<PurchaseRequest> getMarketRequestsForSeller(String seller) {
        List<PurchaseRequest> list = getPurchaseRequests();
        list.removeIf(request -> request.buyerUser != null && request.buyerUser.equals(seller));
        return list;
    }

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

    public boolean addQuote(long requestId, String sellerUser, double price, String description) {
        return addQuote(requestId, sellerUser, price, description, "");
    }

    public boolean addQuote(long requestId, String sellerUser, double price, String description, String images) {
        ContentValues values = new ContentValues();
        values.put("request_id", requestId);
        values.put("seller_user", sellerUser);
        values.put("price", price);
        values.put("description", description);
        values.put("images", images == null ? "" : images);
        values.put("timestamp", System.currentTimeMillis());
        values.put("status", "pending");
        if (hasQuoted(requestId, sellerUser)) {
            return resolver.update(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES, values,
                    "request_id=? AND seller_user=?", new String[] { String.valueOf(requestId), sellerUser }) > 0;
        }
        return resolver.insert(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES, values) != null;
    }

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
        if (quote.images != null && !quote.images.trim().isEmpty()) {
            order.put("proof_images", quote.images);
        }
        if (address != null) {
            order.put("receiver_name", address.receiverName);
            order.put("receiver_phone", address.phone);
            order.put("receiver_address", address.address);
        }
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_ORDERS, order);
        return orderId;
    }

    public boolean rejectQuote(long quoteId, String replyDesc) {
        ContentValues values = new ContentValues();
        values.put("status", "rejected");
        values.put("reply_desc", replyDesc);
        return resolver.update(ZhiNongBaoProvider.CONTENT_URI_PURCHASE_QUOTES, values,
                "id=?", new String[] { String.valueOf(quoteId) }) > 0;
    }

    public boolean hasDefaultAddress(String username) {
        return getDefaultAddress(username) != null;
    }

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

    private String now(String pattern) {
        return new java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(new java.util.Date());
    }

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

    private String[] requestProjection() {
        return new String[] { "id", "buyer_user", "product_name", "category", "quantity", "unit",
                "target_price", "description", "timestamp" };
    }

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
        request.timestamp = cursor.getLong(8);
        return request;
    }

    private String[] quoteProjection() {
        return new String[] { "id", "request_id", "seller_user", "price", "description",
                "images", "timestamp", "status", "reply_desc" };
    }

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
