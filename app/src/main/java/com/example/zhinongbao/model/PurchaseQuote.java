package com.example.zhinongbao.model;

public class PurchaseQuote {
    public long id;
    public long requestId;
    public String requestProductName; // 关联采购需求的商品名
    public String sellerUser;
    public String sellerNickname;
    public double price;
    public String description;
    public String images; // Comma separated URIs from seller quote
    public long timestamp;
    public String status; // pending / accepted / rejected
    public String replyDesc; // 买家同意/拒绝时的备注
}
