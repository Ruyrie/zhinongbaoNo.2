package com.example.zhinongbao.mvp.main;

import android.content.Context;

import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.UserRepository;

public class MainPresenter implements MainContract.Presenter {
    private final UserRepository repository;

    public MainPresenter(Context context, MainContract.View view) {
        this.repository = new UserRepository(context.getApplicationContext());
        view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public int getActiveRole() {
        return repository.getActiveRole();
    }

    @Override
    public boolean isSellerMode() {
        return repository.getActiveRole() == User.ROLE_SELLER;
    }
}
