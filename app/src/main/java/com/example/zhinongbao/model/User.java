package com.example.zhinongbao.model;

/* ============================================================
 * 【用户 / 账号 / User】用户账号模型（Model，数据载体）
 * ============================================================
 * 这个文件是干什么的：
 *   装一个用户的账号信息：用户名、密码、头像、角色（买家/卖家/兼具）。
 *
 * 说明：
 *   - static final int ROLE_xxx：「常量」，给角色编号起好记的名字，避免代码里到处写
 *     0、1、2 看不懂。0=买家、1=卖家、2=两者都是。
 *   - 登录、注册、个人资料、卖家中心等都会用到它。
 *
 * 提示：在 IDE 里搜索「用户」或「登录」可看账号相关文件。
 * ============================================================ */

/** 用户账号模型 */
public class User {
    public static final int ROLE_BUYER = 0;   // 角色：买家
    public static final int ROLE_SELLER = 1;  // 角色：卖家
    public static final int ROLE_BOTH = 2;    // 角色：既是买家也是卖家

    public String username;   // 用户名（登录账号，唯一）
    public String password;   // 密码
    public String avatarUri;  // 头像图片地址
    public int role = ROLE_BUYER; // 角色，默认买家

    // 构造方法：创建用户对象时至少要有用户名和密码
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
