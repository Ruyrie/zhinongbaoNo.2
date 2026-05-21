package com.example.zhinongbao.mvp.settings;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

public class SettingsPresenter implements SettingsContract.Presenter {
    private final SettingsContract.View view;
    private final UserRepository repository;

    public SettingsPresenter(Context context, SettingsContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void logout() {
        repository.logout();
        view.goLogin();
    }
}
