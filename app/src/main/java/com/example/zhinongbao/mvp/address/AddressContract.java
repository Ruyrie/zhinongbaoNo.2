package com.example.zhinongbao.mvp.address;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Address;

import java.util.List;

public interface AddressContract {
    interface View extends BaseView<Presenter> {
        void showAddresses(List<Address> addresses);
        void showToast(String message);
    }

    interface Presenter extends BasePresenter {
        void saveAddress(Address oldAddress, String name, String phone, String address, boolean isDefault, String tag);
        void setDefault(Address address);
        void delete(Address address);
    }
}
