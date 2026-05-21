package com.example.zhinongbao.mvp.main;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface MainContract {
    interface View extends BaseView<Presenter> {
    }

    interface Presenter extends BasePresenter {
        int getActiveRole();
        boolean isSellerMode();
    }
}
