package com.example.zhinongbao.mvp.chat;

import android.content.Context;

import com.example.zhinongbao.model.ChatMessage;
import com.example.zhinongbao.repository.MessageRepository;

public class ChatPresenter implements ChatContract.Presenter {
    private final ChatContract.View view;
    private final MessageRepository repository;
    private final String currentUser;
    private final String otherUser;
    private final String currentNickname;
    private final String otherNickname;
    private final String productName;

    public ChatPresenter(Context context, ChatContract.View view, String otherUser, String productName) {
        this.view = view;
        this.repository = new MessageRepository(context.getApplicationContext());
        this.currentUser = repository.getLoggedUser();
        this.otherUser = otherUser;
        this.productName = productName;
        this.currentNickname = nicknameOrUser(currentUser);
        this.otherNickname = nicknameOrUser(otherUser);
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        if (currentUser == null || currentUser.equals(otherUser)) {
            view.showToast("不能给自己发送消息");
            view.closePage();
            return;
        }
        String title = otherNickname;
        if (productName != null && !productName.isEmpty()) {
            title = title + " · " + productName;
        }
        view.showTitle(title);
        refresh();
    }

    @Override
    public void refresh() {
        view.showMessages(repository.getMessages(currentUser, otherUser), currentUser, currentNickname, otherNickname);
    }

    @Override
    public void markRead() {
        repository.markMessagesRead(otherUser, currentUser);
    }

    @Override
    public void sendMessage(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        repository.sendMessage(currentUser, otherUser, content);
        view.clearInput();
        refresh();
    }

    @Override
    public void sendImage(String imageUri) {
        if (imageUri == null || imageUri.isEmpty()) {
            return;
        }
        repository.sendMessage(currentUser, otherUser, ChatMessage.imageContent(imageUri));
        view.clearInput();
        refresh();
    }

    private String nicknameOrUser(String username) {
        String nickname = repository.getNickname(username);
        return nickname == null || nickname.isEmpty() ? username : nickname;
    }
}
