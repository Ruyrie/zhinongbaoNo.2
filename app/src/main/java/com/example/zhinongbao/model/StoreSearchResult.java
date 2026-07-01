package com.example.zhinongbao.model;

/* ============================================================
 * 【搜索 / StoreSearchResult】店铺搜索结果模型（Model，数据载体）
 * ============================================================
 *
 * 提示：在 IDE 里搜索「搜索」可看搜索相关文件（SearchActivity/ProductSearchActivity）。
 * ============================================================ */

public class StoreSearchResult {
    public String seller;      // 店铺（卖家）用户名
    public String storeName;   // 店铺名称
    public String storePhone;  // 店铺电话
    public String avatarUri;   // 店铺头像
    public int productCount;   // 该店铺在售商品数
}
