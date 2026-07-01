package com.example.zhinongbao.model;

/* ============================================================
 * 【聊天 / 消息 / Chat】单条聊天消息模型（Model，数据载体）
 * ============================================================
 *
 * 亮点：图片消息怎么存？
 *   本项目把图片消息也当作「文字」存，只是在前面加一个特殊前缀 "[image]:"。
 *   - 发图片时：imageContent(图片地址) → 得到 "[image]:图片地址" 存起来。
 *   - 读消息时：isImageContent() 判断是不是图片；imageUri() 把前缀去掉拿回真地址。
 *   这样一张表就能同时存文字和图片消息，简单实用。
 *
 * 提示：在 IDE 里搜索「聊天」或「消息」可看聊天相关文件（ChatActivity/ChatAdapter 等）。
 * ============================================================ */

public class ChatMessage {
    public static final String IMAGE_PREFIX = "[image]:";  // 图片消息的特殊前缀

    public long id;          // 消息 id
    public String fromUser;  // 发送者用户名
    public String toUser;    // 接收者用户名
    public String content;   // 内容（纯文字，或 "[image]:地址" 形式的图片）
    public long timestamp;   // 发送时间戳（毫秒）
    public boolean isRead;   // 对方是否已读
    public boolean recalled; // 是否已撤回（撤回后内容清空，双方都只显示「撤回了一条消息」）

    // 把图片地址包装成「图片消息内容」（加前缀）
    public static String imageContent(String uri) {
        return IMAGE_PREFIX + uri;
    }

    // 判断一段内容是不是图片消息（看是否以前缀开头）
    public static boolean isImageContent(String content) {
        return content != null && content.startsWith(IMAGE_PREFIX);
    }

    // 从图片消息内容里取回真正的图片地址（去掉前缀）；不是图片则返回 null
    public static String imageUri(String content) {
        return isImageContent(content) ? content.substring(IMAGE_PREFIX.length()) : null;
    }

    // 实例方法：判断「本条消息」是不是图片
    public boolean isImage() {
        return isImageContent(content);
    }
}
