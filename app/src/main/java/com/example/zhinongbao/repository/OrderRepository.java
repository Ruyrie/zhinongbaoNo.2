package com.example.zhinongbao.repository;

/* ============================================================
 * 【订单 / Order】数据仓库（Repository，订单全流程数据）
 * ============================================================
 * 这个文件是干什么的：订单功能的核心数据出入口，覆盖买家和卖家两端：
 *   - 买家：查我的订单、确认收货、申请退款、查待评价订单。
 *   - 卖家：查卖出的订单（按状态/关键字）、发货、改价、处理退款、统计销售额。
 *
 * 技术点：
 *   - 所有订单存 orders 表，字段很多（见 orderProjection 里那一长串列名）。
 *   - cursorToOrder：把数据库一行「翻译」成一个 Order 对象（统一在这里做，避免到处重复）。
 *   - 订单状态流转见 Order 模型的 STATUS_xxx 常量。
 *   - autoCompleteExpiredRefunds：每次查询前先跑一遍——卖家超过 24 小时没处理的退款申请，
 *     自动视为同意并完成（保护买家）。
 *   - 金额计算：getOrderPaidAmount = 实付 = 单价(改价优先)×数量 - 折扣。
 *
 * 谁在用它：我的订单、订单详情、卖家订单、卖家销售分析等的 Presenter。
 * 提示：在 IDE 里搜索「订单」可看本组相关文件。
 * ============================================================ */

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

    // 取当前登录用户名
    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    // 买家：取「我的全部订单」（最新在前）
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

    // 买家：取「待评价」订单——已完成、未退款、且我还没评价过该商品
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
                if (!hasProductReview(username, order.productId)) {   // 过滤掉已评价过的
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    // 按订单号查单个订单
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

    // 卖家：取我卖出的全部订单
    public List<Order> getSellerSoldOrders(String seller) {
        return queryOrders("seller=?", new String[] { seller }, "id DESC");
    }

    // 卖家：按状态取我卖出的订单（如只看「待发货」）
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

    // 卖家：按关键字搜索卖出的订单（订单号/商品名/买家名模糊匹配，LIKE %关键字%）
    public List<Order> searchSellerSoldOrders(String seller, String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.isEmpty()) {
            return getSellerSoldOrders(seller);
        }
        String like = "%" + query + "%";   // LIKE 的通配写法：两边 % 表示包含
        return queryOrders(
                "seller=? AND (order_id LIKE ? OR name LIKE ? OR username LIKE ?)",
                new String[] { seller, like, like, like },
                "id DESC");
    }

    // 卖家销售分析：取「已完成且未退款」的订单，scope 控制时间范围（今天/本月/全部）
    public List<Order> getSellerSalesOrders(String seller, String scope) {
        String dateFilter = salesDateFilter(scope);
        // 已退款订单（refund_amount>0）不计入正常销售订单/销售分析
        String selection = "seller=? AND status=? AND refund_amount=0" + dateFilter;
        return queryOrders(selection, new String[] { seller, Order.STATUS_COMPLETED }, "id DESC");
    }

    // 计算一笔订单的实付金额 = 单价(改过价用改后的)×数量 - 折扣，最低 0
    public double getOrderPaidAmount(Order order) {
        double unit = order.unitPrice > 0 ? order.unitPrice : order.price;
        double total = unit * order.quantity - order.discount;
        return Math.max(0, total);
    }

    // 卖家累计订单总数
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

    // 卖家累计总营收 = 所有已完成订单的(实付 - 退款)累加
    public double getTotalRevenueForSeller(String seller) {
        double revenue = 0;
        for (Order order : getSellerSoldOrdersByStatus(seller, Order.STATUS_COMPLETED)) {
            revenue += Math.max(0, getOrderPaidAmount(order) - order.refundAmount);
        }
        return revenue;
    }

    // 卖家某时间范围(今天/本月/全部)的营收
    public double getRevenueForSeller(String seller, String scope) {
        double revenue = 0;
        for (Order order : getSellerSalesOrders(seller, scope)) {
            revenue += Math.max(0, getOrderPaidAmount(order) - order.refundAmount);
        }
        return revenue;
    }

    // 直接更新订单状态（限定本人订单）
    public void updateOrderStatus(String username, String orderId, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        resolver.update(
                ZhiNongBaoProvider.CONTENT_URI_ORDERS,
                values,
                "username=? AND order_id=?",
                new String[] { username, orderId });
    }

    // 买家确认收货：把「已发货」的订单改为「已完成」，并记录完成时间（限定状态防误操作）
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

    // 买家发起退款：记录退款金额/原因/申请时间，保存原状态以便卖家拒绝时还原，状态改为「待处理售后」
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

    // 卖家发货：填好物流信息后把状态改为「已发货」。快递必填公司+单号；自送还需司机电话。
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

    // 卖家改价：设置改后单价和整单折扣
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

    // 卖家处理退款：approve=true 同意（订单标记完成、记下退款额）；false 拒绝（退款额清 0、恢复原状态）
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

    // 判断该用户是否已评价过某商品（用于过滤待评价订单）
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

    // 判断订单当前能否申请退款：已付款/已发货可退；已完成则要在 7 天售后窗口内；已退款/退款中不可再退
    public boolean canRequestRefund(Order order) {
        if (order == null || order.refundAmount > 0 || Order.STATUS_REFUND.equals(order.status)) {
            return false;
        }
        if (Order.STATUS_PAID.equals(order.status) || Order.STATUS_SHIPPED.equals(order.status)) {
            return true;
        }
        return Order.STATUS_COMPLETED.equals(order.status) && isCompletedRefundWindowOpen(order);
    }

    // 判断已完成订单是否还在「7 天售后窗口」内（以完成时间为准，没有则用下单时间）
    public boolean isCompletedRefundWindowOpen(Order order) {
        if (order == null || !Order.STATUS_COMPLETED.equals(order.status)) {
            return false;
        }
        long base = order.completedAt > 0 ? order.completedAt : parseOrderTime(order.time);
        if (base <= 0) {
            return false;
        }
        return System.currentTimeMillis() - base <= 7L * 24 * 60 * 60 * 1000;   // 7 天内
    }

    // 把订单时间字符串解析成毫秒时间戳（失败返回 0）
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

    // 判断字符串是否为空白
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // 通用订单查询：很多方法都调它，传不同的条件/排序，返回订单列表
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

    // 根据范围拼出按时间过滤的 SQL 片段：today→当天、month→当月、其它→不过滤
    private String salesDateFilter(String scope) {
        if ("today".equals(scope)) {
            return " AND time LIKE '" + now("yyyy-MM-dd") + "%'";
        }
        if ("month".equals(scope)) {
            return " AND time LIKE '" + now("yyyy-MM") + "%'";
        }
        return "";
    }

    // 按格式返回当前时间字符串
    private String now(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    // 自动完成「超时未处理」的退款：卖家超过 24 小时没处理的退款申请，自动判为同意并完成订单
    private void autoCompleteExpiredRefunds() {
        long deadline = System.currentTimeMillis() - 24L * 60 * 60 * 1000;   // 24 小时前
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

    // 订单查询要取的所有列名（集中定义，保证每处查询列顺序一致，配合 cursorToOrder 取值）
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

    // 把数据库查询结果的「当前这一行」翻译成一个 Order 对象（列号顺序对应 orderProjection）
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
