package com.example.zhinongbao.mvp.address;

import android.content.Context;

import com.example.zhinongbao.model.Address;
import com.example.zhinongbao.repository.AddressRepository;

/**
 * ============================================================
 * 【收货地址管理 / Address】Presenter（业务逻辑）
 * 整体逻辑（关键步骤）：
 *   1. 构造时创建 AddressRepository、取登录用户名并注入 View。
 *   2. start 拉取该用户地址列表回调 showAddresses。
 *   3. saveAddress 先校验必填项，再写库，成功后 start 刷新。
 *   4. setDefault / delete 调用仓库对应方法后刷新列表。
 * 数据来源：走 repository/AddressRepository；Repository 内部经 ContentProvider
 *   访问 SQLite，本类不直接碰数据库。
 * 配合的文件：接口约定 = AddressContract；View = AddressManagerActivity；模型 = model/Address。
 * 在 MVP 数据流中的位置：Presenter（业务层），承上（View）启下（Repository）。
 * 提示：在 IDE 里搜索「收货地址管理」可看本组相关文件。
 * ============================================================
 */
public class AddressPresenter implements AddressContract.Presenter {
    private final AddressContract.View view;        // 关联的界面
    private final AddressRepository repository;      // 地址数据访问入口
    private final String username;                   // 当前登录用户名

    public AddressPresenter(Context context, AddressContract.View view) {
        this.view = view;
        this.repository = new AddressRepository(context.getApplicationContext());
        this.username = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.showAddresses(repository.getAddresses(username));
    }

    @Override
    public void saveAddress(Address oldAddress, String name, String phone, String address, boolean isDefault, String tag) {
        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            view.showToast("请完整填写地址信息");
            return;
        }
        repository.saveAddress(username, oldAddress == null ? 0 : oldAddress.id, name, phone, address, isDefault, tag);
        start();
    }

    @Override
    public void setDefault(Address address) {
        repository.setDefaultAddress(username, address.id);
        start();
    }

    @Override
    public void delete(Address address) {
        repository.deleteAddress(username, address.id);
        start();
    }
}
