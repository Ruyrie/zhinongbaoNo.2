package com.example.zhinongbao.mvp.address;

import android.content.Context;

import com.example.zhinongbao.model.Address;
import com.example.zhinongbao.repository.AddressRepository;

public class AddressPresenter implements AddressContract.Presenter {
    private final AddressContract.View view;
    private final AddressRepository repository;
    private final String username;

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
    public void saveAddress(Address oldAddress, String name, String phone, String address, boolean isDefault) {
        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            view.showToast("请完整填写地址信息");
            return;
        }
        repository.saveAddress(username, oldAddress == null ? 0 : oldAddress.id, name, phone, address, isDefault);
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
