package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Address;
import com.example.zhinongbao.mvp.address.AddressContract;
import com.example.zhinongbao.mvp.address.AddressPresenter;
import java.util.List;

public class AddressManagerActivity extends BaseMvpActivity<AddressContract.Presenter> implements AddressContract.View {

    private LinearLayout llAddresses;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_manager);
        llAddresses = findViewById(R.id.llAddresses);
        tvEmpty = findViewById(R.id.tvAddressEmpty);
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddAddress).setOnClickListener(v -> showAddressDialog(null));
        new AddressPresenter(this, this).start();
    }

    @Override
    public void showAddresses(List<Address> addresses) {
        llAddresses.removeAllViews();
        tvEmpty.setVisibility(addresses.isEmpty() ? View.VISIBLE : View.GONE);
        for (Address address : addresses) {
            View item = LayoutInflater.from(this).inflate(R.layout.item_address, llAddresses, false);
            ((TextView) item.findViewById(R.id.tvReceiver)).setText(address.receiverName + "  " + address.phone);
            ((TextView) item.findViewById(R.id.tvAddress)).setText(address.address);
            TextView tag = item.findViewById(R.id.tvDefaultTag);
            tag.setVisibility(address.isDefault ? View.VISIBLE : View.GONE);
            item.findViewById(R.id.btnEditAddress).setOnClickListener(v -> showAddressDialog(address));
            item.findViewById(R.id.btnDefaultAddress).setOnClickListener(v -> presenter.setDefault(address));
            item.findViewById(R.id.btnDeleteAddress).setOnClickListener(v -> presenter.delete(address));
            llAddresses.addView(item);
        }
    }

    private void showAddressDialog(Address address) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_address, null);
        EditText etName = view.findViewById(R.id.etReceiverName);
        EditText etPhone = view.findViewById(R.id.etReceiverPhone);
        EditText etAddress = view.findViewById(R.id.etReceiverAddress);
        CheckBox cbDefault = view.findViewById(R.id.cbDefaultAddress);
        if (address != null) {
            etName.setText(address.receiverName);
            etPhone.setText(address.phone);
            etAddress.setText(address.address);
            cbDefault.setChecked(address.isDefault);
        } else {
            cbDefault.setChecked(llAddresses.getChildCount() == 0);
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(address == null ? "新增收货地址" : "编辑收货地址")
                .setView(view)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String addr = etAddress.getText().toString().trim();
            if (name.isEmpty() || phone.isEmpty() || addr.isEmpty()) {
                Toast.makeText(this, "请完整填写地址信息", Toast.LENGTH_SHORT).show();
                return;
            }
            presenter.saveAddress(address, name, phone, addr, cbDefault.isChecked());
            dialog.dismiss();
        }));
        dialog.show();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
