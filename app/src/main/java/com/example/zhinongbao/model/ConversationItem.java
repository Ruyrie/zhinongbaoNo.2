package com.example.zhinongbao.model;

/* ============================================================
 * 【消息 / 会话列表 / Conversation】会话条目模型（Model，数据载体）
 * ============================================================
 *
 * 区别：
 *   - ConversationItem = 聊天「列表」里的一行（一个聊天对象一行）。
 *   - ChatMessage = 进入某个聊天后，里面来回的「一条条」消息。
 *
 * 提示：在 IDE 里搜索「消息」或「会话」可看相关文件（MessageFragment/ConversationAdapter）。
 * ============================================================ */

public class ConversationItem {
    public String otherUser;     // 聊天对方的用户名
    public String displayName;   // 对方显示名称（昵称）
    public String lastMessage;   // 最后一条消息内容（列表里预览）
    public long lastTimestamp;   // 最后一条消息的时间戳（用于排序）
    public int unreadCount;      // 未读消息条数（显示小红点数字）
}
