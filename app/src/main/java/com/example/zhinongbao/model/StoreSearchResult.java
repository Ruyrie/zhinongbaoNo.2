package com.example.zhinongbao.model;

/* ============================================================
 * 【搜索 / StoreSearchResult】店铺搜索结果模型（Model，数据载体）
 * ============================================================
 * 这个文件是干什么的：
 *   搜索店铺时，返回结果列表里的「一家店铺」信息。
 *   和 StoreFootprint 很像，但它没有「浏览时间」，因为它是搜索结果不是足迹。
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
