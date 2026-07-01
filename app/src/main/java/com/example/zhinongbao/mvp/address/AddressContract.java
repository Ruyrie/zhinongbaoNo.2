package com.example.zhinongbao.mvp.address;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Address;

import java.util.List;

/**
 * ============================================================
 * 【收货地址管理 / Address】Contract（接口约定）
 * 约定内容：
 *   - View：showAddresses 刷新地址列表、showToast 弹提示。
 *   - Presenter：saveAddress 新增/编辑保存、setDefault 设默认、delete 删除。
 * 配合的文件：View 实现 = AddressManagerActivity；Presenter 实现 = AddressPresenter；
 *   数据访问 = repository/AddressRepository；模型 = model/Address。
 * 在 MVP 数据流中的位置：接口层，连接 View 与 Presenter。
 * 提示：在 IDE 里搜索「收货地址管理」可看本组相关文件。
 * ============================================================
 */
public interface AddressContract {
    interface View extends BaseView<Presenter> {
        void showAddresses(List<Address> addresses);    // 刷新地址列表
        void showToast(String message);                 // 弹提示
    }

    interface Presenter extends BasePresenter {
        // 保存地址（oldAddress 为 null 表示新增，否则为编辑）
        void saveAddress(Address oldAddress, String name, String phone, String address, boolean isDefault, String tag);
        void setDefault(Address address);   // 设为默认地址
        void delete(Address address);       // 删除地址
    }
}
