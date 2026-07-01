package com.example.zhinongbao.mvp.chat;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.ChatMessage;

import java.util.List;

/**
 * ============================================================
 * 【聊天对话 / Chat】Contract（接口约定）
 * 约定内容：
 *   - View：设置标题、显示消息列表、清空输入框、弹提示、关闭页面。
 *   - Presenter：刷新消息、标记已读、发文字、发图片、撤回消息、删除消息。
 * 关键概念：showMessages 会连同当前用户账号与双方昵称一起传给 View，供适配器区分
 *   左右气泡与显示头像/昵称。
 * 配合的文件：View 实现 = ChatActivity；Presenter 实现 = ChatPresenter；
 *   数据访问 = repository/MessageRepository（经 ContentProvider 访问 SQLite）；
 *   模型 = model/ChatMessage。
 * MVP 数据流位置：本文件是 View 与 Presenter 之间的「合同」。
 * 提示：在 IDE 里搜索「聊天」可看本组相关文件。
 * ============================================================
 */
public interface ChatContract {
    // View：Presenter 用这些方法更新聊天界面
    interface View extends BaseView<Presenter> {
        void showTitle(String title);   // 设置顶部标题（对方昵称，可能带商品名）
        void showMessages(List<ChatMessage> messages, String currentUser, String currentNickname, String otherNickname); // 显示消息列表
        void clearInput();              // 发送后清空输入框
        void showToast(String message); // 弹提示
        void closePage();               // 关闭聊天页
    }

    // Presenter：View（用户操作）用这些方法触发业务
    interface Presenter extends BasePresenter {
        void refresh();                        // 重新读取并刷新消息
        void markRead();                       // 把对方发来的消息标记为已读
        void sendMessage(String content);      // 发送文字消息
        void sendImage(String imageUri);       // 发送图片消息
        void recallMessage(ChatMessage message); // 撤回一条消息（仅本人、2 分钟内）
        void deleteMessage(ChatMessage message); // 删除一条消息（仅从本地视图移除）
    }
}
