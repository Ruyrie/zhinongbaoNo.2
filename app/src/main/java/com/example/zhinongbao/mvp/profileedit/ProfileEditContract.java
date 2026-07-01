package com.example.zhinongbao.mvp.profileedit;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

/**
 * ============================================================
 * 【资料编辑 / ProfileEdit】Contract（接口约定）
 * 约定内容：
 *   - View：回填资料、回填手机号、弹提示、关闭页面。
 *   - Presenter：取当前头像、保存资料、绑定手机号。
 * 配合的文件：View 实现 = ProfileEditActivity；Presenter 实现 = ProfileEditPresenter；
 *   数据访问 = repository/UserRepository。
 * 在 MVP 数据流中的位置：接口层，连接 View 与 Presenter。
 * 提示：在 IDE 里搜索「资料编辑」可看本组相关文件。
 * ============================================================
 */
public interface ProfileEditContract {
    interface View extends BaseView<Presenter> {
        // 回填昵称/签名/头像/手机号到界面
        void showProfile(String nickname, String signature, String avatarUri, String phone);
        void showPhone(String phone);       // 单独刷新手机号显示
        void showToast(String message);     // 弹提示
        void closePage();                   // 保存成功后关闭页面
    }

    interface Presenter extends BasePresenter {
        String getCurrentAvatarUri();       // 取当前头像地址
        void saveProfile(String nickname, String signature, String avatarUri);  // 校验并保存资料
        void bindPhone(String phone);       // 校验并绑定手机号
    }
}
