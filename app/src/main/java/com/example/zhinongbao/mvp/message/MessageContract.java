package com.example.zhinongbao.mvp.message;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.ConversationItem;

import java.util.List;

public interface MessageContract {
    interface View extends BaseView<Presenter> {
        void showConversations(List<ConversationItem> conversations);
    }

    interface Presenter extends BasePresenter {
        void refresh();
        void deleteConversation(String otherUser);
    }
}
