package com.example.zhinongbao.model;

/* ============================================================
 * 【购物车 / Cart】购物车条目模型（Model，数据载体）
 * ============================================================
 *
 * 概念说明：
 *   - Model（模型类）= 只存数据、不含业务逻辑的简单类。
 *   - 字段都是 public，方便各处直接读写（这是本项目的简化写法）。
 *   - 一个 CartItem 对象 = 购物车列表里的一行；很多个 CartItem 组成 List 就是整车商品。
 *
 * 谁在用它：CartAdapter（显示每一行）、CartRepository（存取数据库）、CartPresenter（算钱/下单）。
 * 提示：在 IDE 里搜索「购物车」可看本组全部文件。
 * ============================================================ */

/** 购物车条目模型 */
public class CartItem {
    public int productId;     // 商品 id（唯一标识，用来定位是哪件商品）
    public String name;        // 商品名称
    public double price;       // 单价（一件多少钱）
    public int quantity;       // 购买数量（买几件）
    public String coverUri;    // 封面图地址（用于在列表里显示商品图片）

    // 构造方法①：不传封面图时用这个，封面默认空字符串
    public CartItem(int productId, String name, double price, int quantity) {
        this(productId, name, price, quantity, "");   // 调用下面那个完整构造方法
    }

    // 构造方法②：完整版，创建一个购物车条目时把各字段填好
    public CartItem(int productId, String name, double price, int quantity, String coverUri) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.coverUri = coverUri;
    }
}
