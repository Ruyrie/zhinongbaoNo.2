package com.example.zhinongbao.mvp.profileedit;

import android.content.Context;

import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【资料编辑 / ProfileEdit】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1. 构造时创建 UserRepository、取登录用户名并注入 View。
 *   2. start 拉取昵称/签名/头像/手机号回填界面。
 *   3. saveProfile 校验昵称非空且不超长，仅对有改动的字段写库，按结果提示。
 *   4. bindPhone 校验 11 位手机号后写库，处理「已被其他账号绑定」等失败情况。
 * 数据来源：走 repository/UserRepository；Repository 内部经 ContentProvider
 *   访问 SQLite，本类不直接碰数据库。
 * 配合的文件：接口约定 = ProfileEditContract；View = ProfileEditActivity。
 * 在 MVP 数据流中的位置：Presenter（业务层），承上（View）启下（Repository）。
 * 提示：在 IDE 里搜索「资料编辑」可看本组相关文件。
 * ============================================================
 */
public class ProfileEditPresenter implements ProfileEditContract.Presenter {
    private final ProfileEditContract.View view;    // 关联的界面
    private final UserRepository repository;        // 用户数据访问入口
    private final String username;                  // 当前登录用户名

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
