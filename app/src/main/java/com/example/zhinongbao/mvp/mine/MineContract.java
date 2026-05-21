package com.example.zhinongbao.mvp.mine;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface MineContract {
    interface View extends BaseView<Presenter> {
        void renderUser(String username, String nickname, String signature, String avatarUri,
                int followers, int following, int likes, boolean sellerActive);
        void restartMain();
    }

    interface Presenter extends BasePresenter {
        String getCurrentUser();
        void switchRole();
    }
}
