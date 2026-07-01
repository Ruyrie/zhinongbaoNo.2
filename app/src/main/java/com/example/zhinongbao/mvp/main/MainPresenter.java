package com.example.zhinongbao.mvp.main;

import android.content.Context;

import com.example.zhinongbao.model.User;
import com.example.zhinongbao.repository.UserRepository;

/**
 * ============================================================
 * 【主页面 / Main】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：构造时创建 UserRepository 并把自己注入给 View；
 *   getActiveRole 返回当前激活角色，isSellerMode 判断是否等于卖家角色。
 * 数据来源：走 repository/UserRepository；Repository 内部经 ContentProvider
 *   访问 SQLite，本类不直接碰数据库。
 * 配合的文件：接口约定 = MainContract；View = MainActivity；模型 = model/User。
 * 在 MVP 数据流中的位置：Presenter（业务层），承上（View）启下（Repository）。
 * 提示：在 IDE 里搜索「主页面」可看本组相关文件。
 * ============================================================
 */
public class MainPresenter implements MainContract.Presenter {
    private final UserRepository repository;   // 用户数据访问入口

    public MainPresenter(Context context, MainContract.View view) {
        this.repository = new UserRepository(context.getApplicationContext());
        view.setPresenter(this);
    }

    @Override
    public void start() {
    }

    @Override
    public int getActiveRole() {
        return repository.getActiveRole();
    }

    @Override
    public boolean isSellerMode() {
        return repository.getActiveRole() == User.ROLE_SELLER;
    }
}
