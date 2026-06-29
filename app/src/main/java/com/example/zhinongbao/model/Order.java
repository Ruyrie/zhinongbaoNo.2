package com.example.zhinongbao.model;

/* ============================================================
 * 【订单 / Order】订单模型（Model，数据载体）
 * ============================================================
 * 这个文件是干什么的：
 *   装一笔订单的所有信息：商品、数量、金额、下单时间、当前状态、买卖双方、
 *   物流信息、改价/折扣、售后退款、收货地址等。订单功能的核心数据结构。
 *
 * 重点：订单「状态」是怎么流转的
 *   待付款(pending) → 待发货(paid) → 已发货(shipped) → 已完成(completed)
 *   中途可能：已取消(cancelled)、待处理售后(refund)。
 *   这些状态都用 STATUS_xxx 常量表示，避免代码里到处写 "pending" 这种易错字符串。
 *
 * 订单类型：零售(retail，普通买货) / 供货(procurement，采购市场的供货单)。
 *
 * 提示：在 IDE 里搜索「订单」可看订单相关文件（我的订单/卖家订单/订单详情等）。
 * ============================================================ */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 订单模型 */
public class Order {
    // ↓ 订单状态常量（值是存进数据库的字符串）
    public static final String STATUS_PENDING = "pending"; // 待付款
    public static final String STATUS_PAID = "paid"; // 待发货（已付款）
    public static final String STATUS_SHIPPED = "shipped"; // 已发货
    public static final String STATUS_COMPLETED = "completed"; // 已完成
    public static final String STATUS_REFUND = "refund"; // 待处理售后
    public static final String STATUS_CANCELLED = "cancelled"; // 已取消

    // ↓ 订单类型常量
    public static final String ORDER_TYPE_RETAIL = "retail"; // 零售订单（普通购买）
    public static final String ORDER_TYPE_PROCUREMENT = "procurement"; // 供货订单（采购市场）

    public String orderId;     // 订单号（如 JN1690000000000）
    public int productId;      // 商品 id
    public String name;        // 商品名称
    public double price;       // 单价
    public int quantity;       // 数量
    public String time;        // 下单时间（字符串，格式 yyyy-MM-dd HH:mm）
    public String status;      // 当前状态（取上面的 STATUS_xxx）

    // 买卖双方信息
    public String seller;            // 卖家用户名
    public String buyerUser;         // 买家用户名
    public String buyerNickname;     // 买家昵称
    public String orderType;         // 订单类型：retail（零售）/ procurement（供货）
    public long purchaseRequestId;   // 关联的采购需求 id（供货单才有）

    // 物流信息
    public String shipType;    // 配送方式：express（快递）/ custom（自定义/自送）
    public String shipName;    // 快递公司 / 司机姓名
    public String shipNo;      // 快递单号 / 车牌号
    public String shipPhone;   // 司机电话
    public String proofImages; // 逗号分隔的凭证图片地址

    // 价格调整（卖家可改价/打折）
    public double unitPrice;   // 改价后单价（0 表示没改过）
    public double discount;    // 整单折扣金额

    // 售后/退款相关
    public double refundAmount;          // 退款金额
    public String refundReason;          // 退款原因
    public long refundRequestedAt;       // 申请退款的时间戳
    public String refundPreviousStatus;  // 申请退款前的状态（便于撤销时还原）
    public long completedAt;             // 订单完成时间戳

    // 收货信息
    public String receiverName;     // 收货人姓名
    public String receiverPhone;    // 收货人电话
    public String receiverAddress;  // 收货地址

    // 构造方法：创建一笔订单时填好核心字段，类型默认零售
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

    /**
     * 计算「距离自动超时取消还剩多少毫秒」（下单后 24 小时不付款就算超时）。
     * 返回 <=0 表示已经超时。
     *   - 先把字符串时间解析成 Date，再 +24小时 得到截止时间，
     *     用截止时间减去「现在」就是剩余毫秒数。
     */
    public long getRemainingMs() {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(time);
            if (d == null)
                return -1;
            long deadline = d.getTime() + 24L * 60 * 60 * 1000; // 下单时间 + 24小时
            return deadline - System.currentTimeMillis();        // 截止 - 现在 = 剩余
        } catch (ParseException e) {
            return -1;   // 时间格式异常，当作已超时处理
        }
    }

    /**
     * 实际生效的商品单价：卖家改过价就用改后单价（unitPrice>0），否则用原始下单单价 price。
     * 用于买家侧展示，保证卖家改价后买家看到的也是最新单价。
     */
    public double getEffectiveUnitPrice() {
        return unitPrice > 0 ? unitPrice : price;
    }

    /**
     * 商品总价（折扣前）= 生效单价 × 数量，不减折扣。
     * 用于展示「商品总价」一行，与「实付款」对照，让买家看清减免了多少。
     */
    public double getSubtotal() {
        return getEffectiveUnitPrice() * quantity;
    }

    /**
     * 买家实付金额 = 生效单价 × 数量 - 整单折扣，最低 0。
     * 与 OrderRepository.getOrderPaidAmount 同一口径，供 UI 直接展示，确保卖家改价/折扣后买家同步更新。
     */
    public double getPayableAmount() {
        return Math.max(0, getSubtotal() - discount);
    }
}
