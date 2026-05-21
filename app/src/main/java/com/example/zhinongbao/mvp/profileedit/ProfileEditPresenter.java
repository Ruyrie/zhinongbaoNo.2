package com.example.zhinongbao.mvp.profileedit;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

public class ProfileEditPresenter implements ProfileEditContract.Presenter {
    private final ProfileEditContract.View view;
    private final UserRepository repository;
    private final String username;

    public ProfileEditPresenter(Context context, ProfileEditContract.View view) {
        this.view = view;
        this.repository = new UserRepository(context.getApplicationContext());
        this.username = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.showProfile(repository.getNickname(username), repository.getSignature(username),
                repository.getAvatarUri(username), repository.getPhone(username));
    }

    @Override
    public String getCurrentAvatarUri() {
        return repository.getAvatarUri(username);
    }

    @Override
    public void saveProfile(String nickname, String signature, String avatarUri) {
        if (nickname == null || nickname.isEmpty()) {
            view.showToast("昵称不能为空");
            return;
        }
        if (nickname.length() > 12) {
            view.showToast("昵称不能超过12个字符");
            return;
        }
        boolean success = true;
        if (!nickname.equals(repository.getNickname(username))) {
            success &= repository.setNickname(username, nickname);
        }
        if (!signature.equals(repository.getSignature(username))) {
            success &= repository.updateSignature(username, signature);
        }
        if (avatarUri != null) {
            success &= repository.setAvatarUri(username, avatarUri);
        }
        if (success) {
            view.showToast("保存成功");
            view.closePage();
        } else {
            view.showToast("保存失败，该账号状态异常，请重新登录");
        }
    }

    @Override
    public void bindPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            view.showToast("手机号不能为空");
            return;
        }
        if (phone.length() != 11 || !phone.matches("^1[3-9]\\d{9}$") || phone.matches("^(\\d)\\1{10}$")) {
            view.showToast("请输入有效的11位手机号");
            return;
        }
        if (repository.updatePhone(username, phone)) {
            view.showToast("绑定成功");
            view.showPhone(phone);
        } else {
            view.showToast("该手机号已被其他账号绑定，请更换手机号");
        }
    }
}
