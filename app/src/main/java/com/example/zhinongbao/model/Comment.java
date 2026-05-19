package com.example.zhinongbao.model;

/** 评论模型 */
public class Comment {
    public int id;
    public int articleId;
    public String username;
    public String nickname;
    public String avatarUri;
    public String content;
    public String time;
    public int likeCount;
    public boolean isLikedByMe; // 查询时根据当前登录用户设置，不持久化

    public Comment() {
    }

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
