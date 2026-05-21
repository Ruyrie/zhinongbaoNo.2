package com.example.zhinongbao.base;

import androidx.fragment.app.Fragment;

public abstract class BaseMvpFragment<P extends BasePresenter> extends Fragment implements BaseView<P> {
    protected P presenter;

    @Override
    public void setPresenter(P presenter) {
        this.presenter = presenter;
    }
}
