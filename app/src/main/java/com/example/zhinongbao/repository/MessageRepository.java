package com.example.zhinongbao.repository;

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

    public String getLoggedUser() {
        return prefs().getString(KEY_LOGGED_USER, null);
    }

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

    public void sendMessage(String fromUser, String toUser, String content) {
        if (fromUser == null || toUser == null || fromUser.equals(toUser)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("from_user", fromUser);
        values.put("to_user", toUser);
        values.put("content", content);
        values.put("timestamp", System.currentTimeMillis());
        values.put("is_read", 0);
        resolver.insert(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES, values);
    }

    public List<ChatMessage> getMessages(String user1, String user2) {
        List<ChatMessage> messages = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                new String[] { "id", "from_user", "to_user", "content", "timestamp", "is_read" },
                "(from_user=? AND to_user=?) OR (from_user=? AND to_user=?)",
                new String[] { user1, user2, user2, user1 },
                "timestamp ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                ChatMessage message = new ChatMessage();
                message.id = cursor.getLong(0);
                message.fromUser = cursor.getString(1);
                message.toUser = cursor.getString(2);
                message.content = cursor.getString(3);
                message.timestamp = cursor.getLong(4);
                message.isRead = cursor.getInt(5) == 1;
                messages.add(message);
            }
        }
        return messages;
    }

    public List<ConversationItem> getConversations(String username) {
        LinkedHashMap<String, ConversationItem> map = new LinkedHashMap<>();
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                new String[] { "from_user", "to_user", "content", "timestamp" },
                "from_user=? OR to_user=?",
                new String[] { username, username },
                "timestamp DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                String from = cursor.getString(0);
                String to = cursor.getString(1);
                String other = from.equals(username) ? to : from;
                if (!map.containsKey(other)) {
                    ConversationItem item = new ConversationItem();
                    item.otherUser = other;
                    item.displayName = getNickname(other);
                    item.lastMessage = cursor.getString(2);
                    item.lastTimestamp = cursor.getLong(3);
                    item.unreadCount = getUnreadCount(username, other);
                    map.put(other, item);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    public void markMessagesRead(String fromUser, String toUser) {
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        resolver.update(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES, values,
                "from_user=? AND to_user=?", new String[] { fromUser, toUser });
    }

    public void deleteConversation(String currentUser, String otherUser) {
        resolver.delete(ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                "(from_user=? AND to_user=?) OR (from_user=? AND to_user=?)",
                new String[] { currentUser, otherUser, otherUser, currentUser });
    }

    public int getUnreadCount(String toUser, String fromUser) {
        try (Cursor cursor = resolver.query(
                ZhiNongBaoProvider.CONTENT_URI_CHAT_MESSAGES,
                new String[] { "id" },
                "to_user=? AND from_user=? AND is_read=0",
                new String[] { toUser, fromUser },
                null)) {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE);
    }
}
