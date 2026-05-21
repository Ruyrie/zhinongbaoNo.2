package com.example.zhinongbao.mvp.accountmanager;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.User;

import java.util.List;

public interface AccountManagerContract {
    interface View extends BaseView<Presenter> {
        void showUsers(List<User> users);
        void showCannotRemoveCurrentUser();
    }

    interface Presenter extends BasePresenter {
        void refresh();
        boolean isCurrentUser(String username);
        void hideUserFromHistory(String username);
    }
}
