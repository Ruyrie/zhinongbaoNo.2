package com.example.zhinongbao.mvp.mine;

import android.content.Context;

import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.ArticleRepository;
import com.example.zhinongbao.repository.UserRepository;

public class MinePresenter implements MineContract.Presenter {
    private final MineContract.View view;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    public MinePresenter(Context context, MineContract.View view) {
        Context appContext = context.getApplicationContext();
        this.view = view;
        this.userRepository = new UserRepository(appContext);
        this.articleRepository = new ArticleRepository(appContext);
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        String username = userRepository.getLoggedUser();
        view.renderUser(username, userRepository.getNickname(username), userRepository.getSignature(username),
                userRepository.getAvatarUri(username), articleRepository.getFollowersCount(username),
                articleRepository.getFollowingCount(username), articleRepository.getTotalLikesReceived(username),
                userRepository.getActiveRole() == User.ROLE_SELLER, userRepository.canUseSellerRole(username));
    }

    @Override
    public String getCurrentUser() {
        return userRepository.getLoggedUser();
    }

    @Override
    public void switchRole() {
        String username = userRepository.getLoggedUser();
        if (!userRepository.canUseSellerRole(username)) {
            return;
        }
        boolean sellerActive = userRepository.getActiveRole() == User.ROLE_SELLER;
        userRepository.setActiveRole(sellerActive ? User.ROLE_BUYER : User.ROLE_SELLER);
        view.restartMain();
    }
}
