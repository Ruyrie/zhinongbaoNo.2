package com.example.zhinongbao.model;

/* ============================================================
 * 【采购需求 / PurchaseRequest】采购需求模型（Model，数据载体）
 * ============================================================
 *
 * 提示：在 IDE 里搜索「采购」可看采购相关文件。
 * ============================================================ */

public class PurchaseRequest {
    public long id;             // 需求 id
    public String buyerUser;    // 发布需求的买家用户名
    public String buyerNickname;// 买家昵称
    public String productName;  // 想采购的商品名
    public String category;     // 分类
    public double quantity;     // 采购数量
    public String unit;         // 数量单位（如 斤/箱/吨）
    public double targetPrice;  // 期望/目标单价
    public String description;  // 需求补充说明
    public String images;       // 需求图片（买家发布时上传，逗号分隔多张；同意报价后写入采购订单 proof_images）
    public long timestamp;      // 发布时间戳
    public int quoteCount;      // 已收到的报价条数
}
