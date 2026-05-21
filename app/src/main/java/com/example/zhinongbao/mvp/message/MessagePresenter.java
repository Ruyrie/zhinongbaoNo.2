package com.example.zhinongbao.mvp.message;

import android.content.Context;

import com.example.zhinongbao.repository.MessageRepository;

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
