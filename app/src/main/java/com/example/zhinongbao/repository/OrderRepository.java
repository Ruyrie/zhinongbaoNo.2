package com.example.zhinongbao.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";

    private final Context context;
    private final ContentResolver resolver;

    public OrderRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    public List<Order> getOrders(String username) {
        autoCompleteExpiredRefunds();
        List<Order> orders = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                orderProjection(),
                "username=?",
                new String[] { username },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                orders.add(cursorToOrder(cursor));
            }
        }
        return orders;
    }

    public List<Order> getPendingReviewOrders(String username) {
        autoCompleteExpiredRefunds();
        List<Order> orders = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                orderProjection(),
                "username=? AND product_id>0 AND status=? AND refund_amount=0",
                new String[] { username, Order.STATUS_COMPLETED },
                "id DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                Order order = cursorToOrder(cursor);
                if (!hasProductReview(username, order.productId)) {
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public Order getOrderById(String orderId) {
        autoCompleteExpiredRefunds();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                orderProjection(),
                "order_id=?",
                new String[] { orderId },
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToOrder(cursor);
            }
        }
        return null;
    }

    public List<Order> getSellerSoldOrders(String seller) {
        return queryOrders("seller=?", new String[] { seller }, "id DESC");
    }

    public List<Order> getSellerSoldOrdersByStatus(String seller, String status) {
        return queryOrders("seller=? AND status=?", new String[] { seller, status }, "id DESC");
    }

    /** 已退款订单（卖家已同意退款）：refund_amount>0，与正常销售订单分开统计 */
    public List<Order> getSellerRefundedOrders(String seller) {
        return queryOrders("seller=? AND refund_amount>0", new String[] { seller }, "id DESC");
    }

    /** 统计卖家某一状态下的订单数量（用于红点提醒） */
    public int getSellerOrderCountByStatus(String seller, String status) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "id" },
                "seller=? AND status=?",
                new String[] { seller, status },
                null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    public List<Order> searchSellerSoldOrders(String seller, String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.isEmpty()) {
            return getSellerSoldOrders(seller);
        }
        String like = "%" + query + "%";
        return queryOrders(
                "seller=? AND (order_id LIKE ? OR name LIKE ? OR username LIKE ?)",
                new String[] { seller, like, like, like },
                "id DESC");
    }

    public List<Order> getSellerSalesOrders(String seller, String scope) {
        String dateFilter = salesDateFilter(scope);
        // 已退款订单（refund_amount>0）不计入正常销售订单/销售分析
        String selection = "seller=? AND status=? AND refund_amount=0" + dateFilter;
        return queryOrders(selection, new String[] { seller, Order.STATUS_COMPLETED }, "id DESC");
    }

    public double getOrderPaidAmount(Order order) {
        double unit = order.unitPrice > 0 ? order.unitPrice : order.price;
        double total = unit * order.quantity - order.discount;
        return Math.max(0, total);
    }

    public int getTotalOrderCountForSeller(String seller) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                new String[] { "id" },
                "seller=?",
                new String[] { seller },
                null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    public double getTotalRevenueForSeller(String seller) {
        double revenue = 0;
        for (Order order : getSellerSoldOrdersByStatus(seller, Order.STATUS_COMPLETED)) {
            revenue += Math.max(0, getOrderPaidAmount(order) - order.refundAmount);
        }
        return revenue;
    }

    public double getRevenueForSeller(String seller, String scope) {
        double revenue = 0;
        for (Order order : getSellerSalesOrders(seller, scope)) {
            revenue += Math.max(0, getOrderPaidAmount(order) - order.refundAmount);
        }
        return revenue;
    }

    public void updateOrderStatus(String username, String orderId, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        resolver.update(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                values,
                "username=? AND order_id=?",
                new String[] { username, orderId });
    }

    public boolean confirmReceipt(String username, String orderId) {
        ContentValues values = new ContentValues();
        values.put("status", Order.STATUS_COMPLETED);
        values.put("completed_at", System.currentTimeMillis());
        return resolver.update(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                values,
                "username=? AND order_id=? AND status=?",
                new String[] { username, orderId, Order.STATUS_SHIPPED }) > 0;
    }

    public boolean initiateRefund(String orderId, String reason) {
        Order order = getOrderById(orderId);
        if (order == null || !canRequestRefund(order)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("refund_amount", getOrderPaidAmount(order));
        values.put("refund_reason", reason);
        values.put("refund_requested_at", System.currentTimeMillis());
        values.put("refund_previous_status", order.status);
        values.put("status", Order.STATUS_REFUND);
        return resolver.update(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                values,
                "order_id=?",
                new String[] { orderId }) > 0;
    }

    public boolean shipOrder(String orderId, String shipType, String shipName, String shipNo, String shipPhone) {
        if (isBlank(shipName) || isBlank(shipNo) || ("custom".equals(shipType) && isBlank(shipPhone))) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("status", Order.STATUS_SHIPPED);
        values.put("ship_type", shipType);
        values.put("ship_name", shipName);
        values.put("ship_no", shipNo);
        values.put("ship_phone", shipPhone);
        return resolver.update(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                values,
                "order_id=?",
                new String[] { orderId }) > 0;
    }

    public boolean updateOrderPrice(String orderId, double unitPrice, double discount) {
        ContentValues values = new ContentValues();
        values.put("unit_price", unitPrice);
        values.put("discount", discount);
        return resolver.update(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                values,
                "order_id=?",
                new String[] { orderId }) > 0;
    }

    public boolean processRefund(String orderId, double amount, String reason, boolean approve) {
        ContentValues values = new ContentValues();
        values.put("refund_amount", amount);
        values.put("refund_reason", reason);
        values.put("refund_requested_at", 0);
        if (approve) {
            values.put("status", Order.STATUS_COMPLETED);
        } else {
            Order order = getOrderById(orderId);
            String restore = order != null && order.refundPreviousStatus != null && !order.refundPreviousStatus.isEmpty()
                    ? order.refundPreviousStatus
                    : Order.STATUS_SHIPPED;
            values.put("refund_amount", 0);
            values.put("status", restore);
        }
        values.putNull("refund_previous_status");
        return resolver.update(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                values,
                "order_id=?",
                new String[] { orderId }) > 0;
    }

    private boolean hasProductReview(String username, int productId) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_PRODUCT_COMMENTS,
                new String[] { "id" },
                "username=? AND product_id=?",
                new String[] { username, String.valueOf(productId) },
                null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    public boolean canRequestRefund(Order order) {
        if (order == null || order.refundAmount > 0 || Order.STATUS_REFUND.equals(order.status)) {
            return false;
        }
        if (Order.STATUS_PAID.equals(order.status) || Order.STATUS_SHIPPED.equals(order.status)) {
            return true;
        }
        return Order.STATUS_COMPLETED.equals(order.status) && isCompletedRefundWindowOpen(order);
    }

    public boolean isCompletedRefundWindowOpen(Order order) {
        if (order == null || !Order.STATUS_COMPLETED.equals(order.status)) {
            return false;
        }
        long base = order.completedAt > 0 ? order.completedAt : parseOrderTime(order.time);
        if (base <= 0) {
            return false;
        }
        return System.currentTimeMillis() - base <= 7L * 24 * 60 * 60 * 1000;
    }

    private long parseOrderTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return 0;
        }
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(time);
            return date == null ? 0 : date.getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<Order> queryOrders(String selection, String[] selectionArgs, String sortOrder) {
        autoCompleteExpiredRefunds();
        List<Order> orders = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                orderProjection(),
                selection,
                selectionArgs,
                sortOrder)) {
            while (cursor != null && cursor.moveToNext()) {
                orders.add(cursorToOrder(cursor));
            }
        }
        return orders;
    }

    private String salesDateFilter(String scope) {
        if ("today".equals(scope)) {
            return " AND time LIKE '" + now("yyyy-MM-dd") + "%'";
        }
        if ("month".equals(scope)) {
            return " AND time LIKE '" + now("yyyy-MM") + "%'";
        }
        return "";
    }

    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    private void autoCompleteExpiredRefunds() {
        long deadline = System.currentTimeMillis() - 24L * 60 * 60 * 1000;
        ContentValues values = new ContentValues();
        values.put("status", Order.STATUS_COMPLETED);
        values.put("refund_requested_at", 0);
        values.putNull("refund_previous_status");
        resolver.update(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                values,
                "status=? AND refund_requested_at>0 AND refund_requested_at<=?",
                new String[] { Order.STATUS_REFUND, String.valueOf(deadline) });
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }

    private String[] orderProjection() {
        return new String[] {
                "order_id", "product_id", "name", "price", "quantity", "time", "status",
                "seller", "username", "order_type", "purchase_request_id",
                "ship_type", "ship_name", "ship_no", "ship_phone", "proof_images",
                "unit_price", "discount", "refund_amount", "refund_reason",
                "refund_requested_at", "refund_previous_status",
                "receiver_name", "receiver_phone", "receiver_address", "completed_at"
        };
    }

    private Order cursorToOrder(Cursor cursor) {
        Order order = new Order(
                cursor.getString(0),
                cursor.getInt(1),
                cursor.getString(2),
                cursor.getDouble(3),
                cursor.getInt(4),
                cursor.getString(5),
                cursor.getString(6));
        order.seller = cursor.getString(7);
        order.buyerUser = cursor.getString(8);
        order.buyerNickname = order.buyerUser;
        order.orderType = cursor.getString(9);
        order.purchaseRequestId = cursor.getLong(10);
        order.shipType = cursor.getString(11);
        order.shipName = cursor.getString(12);
        order.shipNo = cursor.getString(13);
        order.shipPhone = cursor.getString(14);
        order.proofImages = cursor.getString(15);
        order.unitPrice = cursor.getDouble(16);
        order.discount = cursor.getDouble(17);
        order.refundAmount = cursor.getDouble(18);
        order.refundReason = cursor.getString(19);
        order.refundRequestedAt = cursor.getLong(20);
        order.refundPreviousStatus = cursor.getString(21);
        order.receiverName = cursor.getString(22);
        order.receiverPhone = cursor.getString(23);
        order.receiverAddress = cursor.getString(24);
        order.completedAt = cursor.getLong(25);
        if (order.orderType == null) {
            order.orderType = Order.ORDER_TYPE_RETAIL;
        }
        return order;
    }
}
