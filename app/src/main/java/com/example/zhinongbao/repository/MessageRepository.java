package com.example.zhinongbao.repository;

/* ============================================================
 * 【消息 / 聊天 / Message】数据仓库（Repository，私信聊天数据）
 * ============================================================
 *
 * 技术点：
 *   - 一张 chat_messages 表存所有人的消息，靠 from_user/to_user 区分。
 *   - 取两人聊天记录的条件「(我发他) 或 (他发我)」，所以 SQL 里用 OR 两段。
 *   - getConversations 用 LinkedHashMap 按时间倒序遍历、每个对象只留第一条（即最新一条），
 *     既去重又保持顺序。图片消息在列表里显示为「[图片]」。
 *
 * 谁在用它：MessagePresenter（消息列表）、ChatPresenter（聊天页）。
 * 提示：在 IDE 里搜索「消息」或「聊天」可看本组相关文件。
 * ============================================================ */

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.zhinongbao.model.ChatMessage;
import com.example.zhinongbao.model.ConversationItem;
import com.example.zhinongbao.provider.ZhiNongBaoProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MessageRepository {
    private static final String PREF_SESSION = "pref_session";
    private static final String KEY_LOGGED_USER = "logged_user";

    private final Context context;
    private final ContentResolver resolver;

    public MessageRepository(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = this.context.getContentResolver();
    }

    // 取当前登录用户名
    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

    // 取某用户的昵称（用于在消息列表显示对方名字）
    public String getNickname(String username) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_USERS,
                new String[] { "nickname" },
                "username=?",
                new String[] { username },
                null)) {
            return cursor != null && cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    // 发送消息：插入一条新消息（初始未读 is_read=0）。不能给自己发。
    public void sendMessage(String fromUser, String toUser, String content) {
        if (fromUser == null || toUser == null || fromUser.equals(toUser)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("from_user", fromUser);
        values.put("to_user", toUser);
        values.put("content", content);
        values.put("timestamp", System.currentTimeMillis());   // 当前时间戳
        values.put("is_read", 0);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES, values);
    }

    // 取两人之间的全部聊天记录，按时间从早到晚（timestamp ASC）。
    //   第一个参数 user1 是「当前查看者」：会自动过滤掉「他在自己这一侧删除过」的消息
    //   （我作为发送方删 → deleted_by_from；我作为接收方删 → deleted_by_to）。对方仍能看到这些消息。
    public List<ChatMessage> getMessages(String user1, String user2) {
        List<ChatMessage> messages = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                new String[] { "id", "from_user", "to_user", "content", "timestamp", "is_read", "recalled" },
                "((from_user=? AND to_user=?) OR (from_user=? AND to_user=?))"
                        + " AND NOT (from_user=? AND deleted_by_from=1)"
                        + " AND NOT (to_user=? AND deleted_by_to=1)",
                new String[] { user1, user2, user2, user1, user1, user1 },
                "timestamp ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                ChatMessage message = new ChatMessage();
                message.id = cursor.getLong(0);
                message.fromUser = cursor.getString(1);
                message.toUser = cursor.getString(2);
                message.content = cursor.getString(3);
                message.timestamp = cursor.getLong(4);
                message.isRead = cursor.getInt(5) == 1;
                message.recalled = cursor.getInt(6) == 1;
                messages.add(message);
            }
        }
        return messages;
    }

    // 撤回一条消息：标记 recalled=1 并清空内容（双方都只看到「撤回了一条消息」，图片地址也一并清掉）。
    // 「只能本人撤回、且 2 分钟内」由 ChatPresenter 校验，这里只负责落库。
    public void recallMessage(long messageId) {
        ContentValues values = new ContentValues();
        values.put("recalled", 1);
        values.put("content", "");
        values.put("is_read", 1);   // 撤回后不再计入对方的未读数
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES, values,
                "id=?", new String[] { String.valueOf(messageId) });
    }

    // 删除一条消息（只影响 viewer 自己这一侧，对方仍可见）：
    //   viewer 是发送方 → 置 deleted_by_from=1；viewer 是接收方 → 置 deleted_by_to=1。
    //   两条 update 各带 from_user/to_user 条件，只会命中对应的那一侧，互不影响。
    public void deleteMessageForUser(long messageId, String viewer) {
        String id = String.valueOf(messageId);

        ContentValues asSender = new ContentValues();
        asSender.put("deleted_by_from", 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES, asSender,
                "id=? AND from_user=?", new String[] { id, viewer });

        ContentValues asReceiver = new ContentValues();
        asReceiver.put("deleted_by_to", 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES, asReceiver,
                "id=? AND to_user=?", new String[] { id, viewer });
    }

    // 取消息列表：把「与每个人的最新一条消息」聚成一行。按时间倒序遍历，每个对象只留首次出现（即最新）。
    public List<ConversationItem> getConversations(String username) {
        LinkedHashMap<String, ConversationItem> map = new LinkedHashMap<>();   // 保持插入顺序的 Map
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                new String[] { "from_user", "to_user", "content", "timestamp", "recalled" },
                // 过滤掉「我自己这一侧删除过」的消息，列表的「最新一条」才不会显示已删内容
                "(from_user=? OR to_user=?)"
                        + " AND NOT (from_user=? AND deleted_by_from=1)"
                        + " AND NOT (to_user=? AND deleted_by_to=1)",
                new String[] { username, username, username, username },
                "timestamp DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                String from = cursor.getString(0);
                String to = cursor.getString(1);
                String other = from.equals(username) ? to : from;   // 对方 = 这条消息里不是我的那个人
                if (!map.containsKey(other)) {   // 该对象第一次出现（因倒序，必为最新消息）
                    ConversationItem item = new ConversationItem();
                    item.otherUser = other;
                    item.displayName = getNickname(other);
                    String content = cursor.getString(2);
                    boolean recalled = cursor.getInt(4) == 1;
                    if (recalled) {
                        item.lastMessage = from.equals(username) ? "你撤回了一条消息" : "对方撤回了一条消息";
                    } else {
                        item.lastMessage = ChatMessage.isImageContent(content) ? "[图片]" : content;
                    }
                    item.lastTimestamp = cursor.getLong(3);
                    item.unreadCount = getUnreadCount(username, other);
                    map.put(other, item);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    // 把「fromUser 发给 toUser」的消息全部标记为已读（进入聊天页时调用）
    public void markMessagesRead(String fromUser, String toUser) {
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES, values,
                "from_user=? AND to_user=?", new String[] { fromUser, toUser });
    }

    // 删除两人的整段会话（双向消息都删）
    public void deleteConversation(String currentUser, String otherUser) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                "(from_user=? AND to_user=?) OR (from_user=? AND to_user=?)",
                new String[] { currentUser, otherUser, otherUser, currentUser });
    }

    // 统计「fromUser 发给 toUser、尚未读」的消息条数（getCount = 查询结果行数）
    public int getUnreadCount(String toUser, String fromUser) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                new String[] { "id" },
                "to_user=? AND from_user=? AND is_read=0 AND recalled=0 AND deleted_by_to=0",
                new String[] { toUser, fromUser },
                null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    // 统计「发给 username、未读、未撤回、未被自己删除」的全部消息条数（用于底部导航「消息」红点）
    public int getTotalUnreadCount(String username) {
        if (username == null) {
            return 0;
        }
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                new String[] { "id" },
                "to_user=? AND is_read=0 AND recalled=0 AND deleted_by_to=0",
                new String[] { username },
                null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }
}
