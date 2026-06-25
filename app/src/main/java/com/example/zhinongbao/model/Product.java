package com.example.zhinongbao.model;

/* ============================================================
 * 【商品 / Product】商品模型（Model，数据载体）
 * ============================================================
 * 这个文件是干什么的：
 *   一个「数据盒子」，装一件商品的全部信息：名称、描述、价格、图片、
 *   分类、卖家、品牌、产地、规格、包装、浏览量等。
 *
 * 说明：
 *   - 它被几乎整个「商城」相关功能用到（商品列表、详情、搜索、收藏、购物车、下单）。
 *   - 下面有好几个「构造方法」（同名但参数个数不同）= 重载：不同场景按需创建，
 *     有时只需基本信息，有时需要完整信息。后面的构造方法用 this(...) 复用前面的。
 *
 * 提示：在 IDE 里搜索「商品」可看本组相关文件（详情/搜索/收藏/列表适配器等）。
 * ============================================================ */

/** 商品模型 */
public class Product {
    public int id;             // 商品唯一 id
    public String name;        // 商品名称
    public String desc;        // 商品描述/简介
    public double price;       // 价格
    public String coverUri;    // 封面图地址（可能是多张，逗号分隔）
    public String category;    // 逗号分隔的分类字符串，如 "推荐,水果蔬菜"
    public String seller;      // 卖家用户名
    public String brand;       // 品牌
    public String origin;      // 产地
    public String spec;        // 规格（如 500g/袋）
    public String packageType; // 包装方式
    public int viewCount;      // 浏览次数
    public long viewedAt;      // 最近浏览时间（用于「足迹」排序）

    // 构造方法①：最简版，只有 id/名称/描述/价格，分类默认「推荐」
    public Product(int id, String name, String desc, double price) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.category = "推荐";
    }

    // 构造方法②：在①基础上多了封面图
    public Product(int id, String name, String desc, double price, String coverUri) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.coverUri = coverUri;
        this.category = "推荐";
    }

    // 构造方法③：可以自己指定分类
    public Product(int id, String name, String desc, double price, String coverUri, String category) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.coverUri = coverUri;
        this.category = category;
    }

    // 构造方法④：在③基础上加卖家、浏览量（this(...) 复用③，少写重复代码）
    public Product(int id, String name, String desc, double price, String coverUri,
            String category, String seller, int viewCount) {
        this(id, name, desc, price, coverUri, category);
        this.seller = seller;
        this.viewCount = viewCount;
    }

    // 构造方法⑤：最完整版，连品牌/产地/规格/包装都带上（商品详情页用）
    public Product(int id, String name, String desc, double price, String coverUri,
            String category, String seller, int viewCount, String brand, String origin,
            String spec, String packageType) {
        this(id, name, desc, price, coverUri, category, seller, viewCount);
        this.brand = brand;
        this.origin = origin;
        this.spec = spec;
        this.packageType = packageType;
    }
}
