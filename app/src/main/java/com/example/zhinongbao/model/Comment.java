package com.example.zhinongbao.model;

/* ============================================================
 * 【评论 / Comment】文章评论模型（Model，数据载体）
 * ============================================================
 *
 * 说明：
 *   - isLikedByMe：表示「当前登录的我」有没有给这条评论点过赞。它是查询时临时算出来的，
 *     不存数据库——所以注释写「不持久化」。
 *   - 有两个构造方法：空构造（先 new 再逐字段赋值）和带参构造（一次填好）。
 *
 * 提示：在 IDE 里搜索「评论」可看评论相关文件（CommentAdapter/文章详情等）。
 * ============================================================ */

/** 评论模型 */
public class Comment {
    public int id;            // 评论 id
    public int articleId;     // 所属文章 id
    public String username;   // 评论者用户名
    public String nickname;   // 评论者昵称（显示用）
    public String avatarUri;  // 评论者头像（显示用）
    public String content;    // 评论内容
    public String time;       // 评论时间
    public int likeCount;     // 点赞数
    public boolean isLikedByMe; // 当前登录用户是否点过赞（查询时计算，不存数据库）

    // 空构造方法：先创建空对象，再一个个字段赋值
    public Comment() {
    }

    // 带参构造方法：一次性填好核心字段
    public Comment(int id, int articleId, String username,
            String content, String time, int likeCount) {
        this.id = id;
        this.articleId = articleId;
        this.username = username;
        this.content = content;
        this.time = time;
        this.likeCount = likeCount;
    }
}
