package com.example.zhinongbao.mvp.followlist;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

import java.util.List;

public interface FollowListContract {
    interface View extends BaseView<Presenter> {
        void showUsers(List<String> users, String currentUser);
        void showFollowing(List<String> userFollows, List<String> storeFollows, String currentUser);
    }

    interface Presenter extends BasePresenter {
        String getNickname(String username);
        String getAvatarUri(String username);
        String getStoreName(String username);
        boolean isStoreAccount(String username);
        boolean isFollowing(String username, boolean store);
        void toggleFollow(String username, boolean store);
    }
}
