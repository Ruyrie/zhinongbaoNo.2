package com.example.zhinongbao.model;

public class ChatMessage {
    public static final String IMAGE_PREFIX = "[image]:";

    public long id;
    public String fromUser;
    public String toUser;
    public String content;
    public long timestamp;
    public boolean isRead;

    public static String imageContent(String uri) {
        return IMAGE_PREFIX + uri;
    }

    public static boolean isImageContent(String content) {
        return content != null && content.startsWith(IMAGE_PREFIX);
    }

    public static String imageUri(String content) {
        return isImageContent(content) ? content.substring(IMAGE_PREFIX.length()) : null;
    }

    public boolean isImage() {
        return isImageContent(content);
    }
}
