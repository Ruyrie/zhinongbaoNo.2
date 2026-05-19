package com.example.zhinongbao.model;

/** 用户账号模型 */
public class User {
    public static final int ROLE_BUYER = 0;
    public static final int ROLE_SELLER = 1;
    public static final int ROLE_BOTH = 2;

    public String username;
    public String password;
    public String avatarUri;
    public int role = ROLE_BUYER; // 默认买家

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
