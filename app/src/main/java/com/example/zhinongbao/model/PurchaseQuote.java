package com.example.zhinongbao.model;

/* ============================================================
 * 【采购报价 / PurchaseQuote】采购报价模型（Model，数据载体）
 * ============================================================
 * 这个文件是干什么的：
 *   在「采购市场」里，买家发布采购需求(PurchaseRequest)，卖家针对需求给出报价，
 *   一条报价就是一个 PurchaseQuote：谁报的、报多少钱、配图、状态。
 *
 * 说明：报价状态 status
 *   pending（待处理）/ accepted（买家已接受）/ rejected（买家已拒绝）。
 *
 * 提示：在 IDE 里搜索「采购」可看采购相关文件（采购市场/发布采购/卖家采购管理）。
 * ============================================================ */

public class PurchaseQuote {
    public long id;                    // 报价 id
    public long requestId;             // 对应的采购需求 id
    public String requestProductName;  // 关联采购需求的商品名
    public String sellerUser;          // 报价卖家用户名
    public String sellerNickname;      // 报价卖家昵称
    public double price;               // 报价金额
    public String description;         // 报价说明
    public String images;              // 配图：逗号分隔的多个图片地址
    public long timestamp;             // 报价时间戳
    public String status;              // 状态：pending/accepted/rejected
    public String replyDesc;           // 买家同意/拒绝时填的备注
}
