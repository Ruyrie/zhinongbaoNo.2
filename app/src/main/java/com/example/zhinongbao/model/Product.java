package com.example.zhinongbao.model;

/** 商品模型 */
public class Product {
    public int id;
    public String name;
    public String desc;
    public double price;
    public String coverUri;
    public String category; // 逗号分隔的分类字符串，如 "推荐,水果蔬菜"

    public Product(int id, String name, String desc, double price) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.category = "推荐";
    }

    public Product(int id, String name, String desc, double price, String coverUri) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.coverUri = coverUri;
        this.category = "推荐";
    }

    public Product(int id, String name, String desc, double price, String coverUri, String category) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.coverUri = coverUri;
        this.category = category;
    }
}
