package com.example.zhinongbao.model;

/* ============================================================
 * 【足迹 / StoreFootprint】店铺浏览足迹模型（Model，数据载体）
 * ============================================================
 * 这个文件是干什么的：
 *   记录「我最近看过的店铺」一条：店铺信息 + 我浏览它的时间(viewedAt)，
 *   用于「足迹」页按时间倒序展示我逛过哪些店。
 *
 * 提示：在 IDE 里搜索「足迹」可看相关文件（FootprintActivity）。
 * ============================================================ */

public class StoreFootprint {
    public String seller;      // 店铺（卖家）用户名
    public String storeName;   // 店铺名称
    public String storePhone;  // 店铺电话
    public String avatarUri;   // 店铺头像
    public int productCount;   // 该店铺在售商品数
    public long viewedAt;      // 我浏览这家店的时间戳（用于排序）
}
