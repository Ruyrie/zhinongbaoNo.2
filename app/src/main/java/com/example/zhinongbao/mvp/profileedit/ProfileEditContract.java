package com.example.zhinongbao.mvp.profileedit;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface ProfileEditContract {
    interface View extends BaseView<Presenter> {
        void showProfile(String nickname, String signature, String avatarUri, String phone);
        void showPhone(String phone);
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        String getCurrentAvatarUri();
        void saveProfile(String nickname, String signature, String avatarUri);
        void bindPhone(String phone);
    }
}
