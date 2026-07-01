package com.example.zhinongbao.model;

/* ============================================================
 * 【收货地址 / Address】收货地址模型（Model，数据载体）
 * ============================================================
 *
 * 提示：在 IDE 里搜索「地址」可看地址相关文件（AddressManagerActivity 等）。
 * ============================================================ */

public class Address {
    public long id;              // 地址 id（数据库主键）
    public String username;      // 属于哪个用户
    public String receiverName;  // 收货人姓名
    public String phone;         // 收货人电话
    public String address;       // 详细地址
    public boolean isDefault;    // 是否为默认地址（结算时优先用默认地址）
    public String tag;           // 地址标签（家/公司/学校/父母/朋友/自定义），可为空
}
