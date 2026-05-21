package com.example.zhinongbao.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 订单模型 */
public class Order {
    public static final String STATUS_PENDING = "pending"; // 待付款
    public static final String STATUS_PAID = "paid"; // 待发货（已付款）
    public static final String STATUS_SHIPPED = "shipped"; // 已发货
    public static final String STATUS_COMPLETED = "completed"; // 已完成
    public static final String STATUS_REFUND = "refund"; // 待处理售后
    public static final String STATUS_CANCELLED = "cancelled"; // 已取消

    public static final String ORDER_TYPE_RETAIL = "retail"; // 零售订单
    public static final String ORDER_TYPE_PROCUREMENT = "procurement"; // 供货订单

    public String orderId;
    public int productId;
    public String name;
    public double price;
    public int quantity;
    public String time;
    public String status;

    // 卖家信息
    public String seller;
    public String buyerUser;
    public String buyerNickname;
    public String orderType; // retail / procurement
    public long purchaseRequestId;

    // 物流信息
    public String shipType; // express / custom
    public String shipName; // 快递公司 / 司机姓名
    public String shipNo; // 快递单号 / 车牌号
    public String shipPhone; // 司机电话
    public String proofImages; // 逗号分隔的凭证图片 URI

    // 价格调整
    public double unitPrice; // 改价后单价（0 表示未改）
    public double discount; // 整单折扣金额

    // 售后
    public double refundAmount;
    public String refundReason;
    public long refundRequestedAt;
    public String refundPreviousStatus;

    public String receiverName;
    public String receiverPhone;
    public String receiverAddress;

    public Order(String orderId, int productId, String name, double price, int quantity, String time, String status) {
        this.orderId = orderId;
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.time = time;
        this.status = status;
        this.orderType = ORDER_TYPE_RETAIL;
    }

    /** 距离超时取消剩余毫秒数（24h），<=0 表示已超时 */
    public long getRemainingMs() {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(time);
            if (d == null)
                return -1;
            long deadline = d.getTime() + 24L * 60 * 60 * 1000;
            return deadline - System.currentTimeMillis();
        } catch (ParseException e) {
            return -1;
        }
    }
}
