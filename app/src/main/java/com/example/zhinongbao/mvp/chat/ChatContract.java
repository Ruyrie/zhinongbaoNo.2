package com.example.zhinongbao.mvp.chat;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.ChatMessage;

import java.util.List;

public interface ChatContract {
    interface View extends BaseView<Presenter> {
        void showTitle(String title);
        void showMessages(List<ChatMessage> messages, String currentUser, String currentNickname, String otherNickname);
        void clearInput();
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void refresh();
        void markRead();
        void sendMessage(String content);
    }
}
