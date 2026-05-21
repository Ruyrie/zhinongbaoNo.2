package com.example.zhinongbao.mvp.password;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface ChangePasswordContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void save(String username, String newPwd, String confirmPwd);
    }
}
