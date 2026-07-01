package com.example.zhinongbao.mvp.message;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.ConversationItem;

import java.util.List;

/**
 * ============================================================
 * 【消息列表 / Message】Contract（接口约定）
 * 约定内容：
 *   - View：显示会话列表 showConversations。
 *   - Presenter：刷新会话列表 refresh、删除某段会话 deleteConversation。
 * 配合的文件：View 实现 = MessageFragment；Presenter 实现 = MessagePresenter；
 *   数据访问 = repository/MessageRepository（经 ContentProvider 访问 SQLite）；
 *   模型 = model/ConversationItem。
 * MVP 数据流位置：本文件是买卖双方（View 与 Presenter）之间的「合同」。
 * 提示：在 IDE 里搜索「消息」可看本组相关文件。
 * ============================================================
 */
public interface MessageContract {
    // View：Presenter 用这些方法更新消息列表界面
    interface View extends BaseView<Presenter> {
        void showConversations(List<ConversationItem> conversations); // 显示会话列表
    }

    // Presenter：View（用户操作）用这些方法触发业务
    interface Presenter extends BasePresenter {
        void refresh();                             // 重新读取并刷新会话列表
        void deleteConversation(String otherUser);  // 删除与某人的整段会话
    }
}
