package com.example.zhinongbao.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseMvpActivity<P extends BasePresenter> extends AppCompatActivity implements BaseView<P> {
    protected P presenter;

    @Override
    public void setPresenter(P presenter) {
        this.presenter = presenter;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
