package com.example.zhinongbao.mvp.message;

import android.content.Context;

import com.example.zhinongbao.repository.MessageRepository;

/**
 * ============================================================
 * 【消息列表 / Message】Presenter（业务逻辑）
 * 整体逻辑：构造时创建 MessageRepository 并取出当前登录账号 currentUser，
 *   把自己注册给 View；start/refresh 拉取该用户的全部会话交 View 渲染（未登录则不处理）；
 *   deleteConversation 删库后再 refresh 刷新列表。
 * 数据来源：repository/MessageRepository；Repository 内部经 ContentProvider 访问 SQLite。
 *   本类不直接碰数据库。
 * 配合的文件：接口约定 MessageContract；View 实现 = MessageFragment；
 *   数据模型 model/ConversationItem。
 * MVP 数据流位置：本类是 Presenter，居中协调 View 与 Repository。
 * 提示：在 IDE 里搜索「消息」可看本组相关文件。
 * ============================================================
 */
public class MessagePresenter implements MessageContract.Presenter {
    private final MessageContract.View view;
    private final MessageRepository repository;
    private final String currentUser;

    public MessagePresenter(Context context, MessageContract.View view) {
        this.view = view;
        this.repository = new MessageRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        if (currentUser == null) {
            return;
        }
        view.showConversations(repository.getConversations(currentUser));
    }

    @Override
    public void deleteConversation(String otherUser) {
        repository.deleteConversation(currentUser, otherUser);
        refresh();
    }
}
