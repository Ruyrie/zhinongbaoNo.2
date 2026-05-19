package com.example.zhinongbao.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 订单模型 */
public class Order {
    public static final String STATUS_PENDING   = "pending";   // 待支付
    public static final String STATUS_PAID      = "paid";      // 已支付/完成
    public static final String STATUS_CANCELLED = "cancelled"; // 已取消

    public String orderId;   // 订单号
    public int productId;
    public String name;
    public double price;
    public int quantity;
    public String time;      // 下单时间 "yyyy-MM-dd HH:mm"
    public String status;    // pending / paid / cancelled

    public Order(String orderId, int productId, String name, double price, int quantity, String time, String status) {
        this.orderId    = orderId;
        this.productId  = productId;
        this.name       = name;
        this.price      = price;
        this.quantity   = quantity;
        this.time       = time;
        this.status     = status;
    }

    /** 距离超时取消剩余毫秒数（24h），<=0 表示已超时 */
    public long getRemainingMs() {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(time);
            if (d == null) return -1;
            long deadline = d.getTime() + 24L * 60 * 60 * 1000;
            return deadline - System.currentTimeMillis();
        } catch (ParseException e) { return -1; }
    }
}
