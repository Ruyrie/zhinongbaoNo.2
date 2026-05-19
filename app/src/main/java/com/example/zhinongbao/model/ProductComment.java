package com.example.zhinongbao.model;

public class ProductComment {
    public int id;
    public int productId;
    public String username;
    public String content;
    public String images; // Comma separated URIs
    public String time;

    // Additional fields for UI
    public String nickname;
    public String avatarUri;

    public ProductComment(int id, int productId, String username, String content, String images, String time) {
        this.id = id;
        this.productId = productId;
        this.username = username;
        this.content = content;
        this.images = images;
        this.time = time;
    }
}
