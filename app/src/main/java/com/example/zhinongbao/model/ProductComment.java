package com.example.zhinongbao.model;

/* ============================================================
 * 【商品评价 / ProductComment】商品评价模型（Model，数据载体）
 * ============================================================
 * 这个文件是干什么的：
 *   装一条「商品的评价/晒单」：评的是哪个商品、谁评的、文字内容、配图、时间。
 *   （注意：和「文章评论 Comment」不是一回事，这个是针对商品的。）
 *
 * 说明：
 *   - images 是「逗号分隔的多张图片地址」存在一个字符串里，用时再 split 拆开。
 *   - nickname/avatarUri 是显示用的附加信息，查询时一并带出。
 *
 * 提示：在 IDE 里搜索「商品评价」可看相关文件（ProductCommentAdapter/评价页/发表评价）。
 * ============================================================ */

public class ProductComment {
    public int id;           // 评价 id
    public int productId;    // 评价的是哪个商品
    public String username;  // 评价者用户名
    public String content;   // 评价文字
    public String images;    // 配图：逗号分隔的多个图片地址
    public String time;      // 评价时间

    // 给界面显示用的附加字段（查询时带出）
    public String nickname;   // 评价者昵称
    public String avatarUri;  // 评价者头像

    // 构造方法：一次填好核心字段
    public ProductComment(int id, int productId, String username, String content, String images, String time) {
        this.id = id;
        this.productId = productId;
        this.username = username;
        this.content = content;
        this.images = images;
        this.time = time;
    }
}
