package com.example.zhinongbao.mvp.accountmanager;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

public class AccountManagerPresenter implements AccountManagerContract.Presenter {
    private final AccountManagerContract.View view;
    private final UserRepository repository;

    public AccountManagerPresenter(Context context, AccountManagerContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        refresh();
    }

    @Override
    public void refresh() {
        view.showUsers(repository.getUsers());
    }

    @Override
    public boolean isCurrentUser(String username) {
        return username != null && username.equals(repository.getLoggedUser());
    }

    @Override
    public void hideUserFromHistory(String username) {
        repository.hideUserFromHistory(username);
        refresh();
    }
}
