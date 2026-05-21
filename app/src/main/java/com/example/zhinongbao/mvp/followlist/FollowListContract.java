package com.example.zhinongbao.mvp.followlist;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

import java.util.List;

public interface FollowListContract {
    interface View extends BaseView<Presenter> {
        void showUsers(List<String> users, String currentUser);
    }

    interface Presenter extends BasePresenter {
        String getNickname(String username);
        String getAvatarUri(String username);
        boolean isFollowing(String username);
        void toggleFollow(String username);
    }
}
