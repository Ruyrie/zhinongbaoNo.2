package com.example.zhinongbao.mvp.settings;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface SettingsContract {
    interface View extends BaseView<Presenter> {
        void goLogin();
    }

    interface Presenter extends BasePresenter {
        void logout();
    }
}
