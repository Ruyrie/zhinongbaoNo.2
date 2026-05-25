package com.example.zhinongbao.model;

/** 购物车条目模型 */
public class CartItem {
    public int productId;
    public String name;
    public double price;
    public int quantity;
    public String coverUri;

    public CartItem(int productId, String name, double price, int quantity) {
        this(productId, name, price, quantity, "");
    }

    public CartItem(int productId, String name, double price, int quantity, String coverUri) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.coverUri = coverUri;
    }
}
