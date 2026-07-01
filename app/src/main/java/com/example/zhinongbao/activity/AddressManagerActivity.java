package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Address;
import com.example.zhinongbao.mvp.address.AddressContract;
import com.example.zhinongbao.mvp.address.AddressPresenter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * ============================================================
 * 【收货地址管理 / Address】View（地址管理页 Activity）
 * 整体逻辑（关键步骤）：
 *   1. onCreate 加载 activity_address_manager 布局，new AddressPresenter 拉取地址。
 *   2. showAddresses 用 item_address 逐条渲染地址卡片并绑定编辑/默认/删除按钮。
 *   3. 新增或编辑时弹全屏 dialog_address，含收件人、电话、地区、详细地址、标签胶囊、默认开关。
 *   4. 点「获取本地位置」先申请定位权限，再用 LocationManager 取一次坐标，
 *      经 Geocoder 反向解析为中文地址（子线程处理）后回填输入框。
 *   5. 点保存把信息交 presenter.saveAddress 校验并写库。
 * 数据来源：经 AddressPresenter 走 AddressRepository；Repository 内部经
 *   ContentProvider 访问 SQLite，本类不直接碰数据库。定位用系统 LocationManager/Geocoder。
 * 配合的文件：接口约定 = mvp/address/AddressContract；业务逻辑 = mvp/address/AddressPresenter；
 *   布局 = res/layout/activity_address_manager.xml、item_address.xml、dialog_address.xml；
 *   模型 = model/Address。
 * 在 MVP 数据流中的位置：View（界面层），通过 Presenter 读写地址，不直接访问数据库。
 * 提示：在 IDE 里搜索「收货地址管理」可看本组相关文件。
 * ============================================================
 */
public class AddressManagerActivity extends BaseMvpActivity<AddressContract.Presenter> implements AddressContract.View {

    /** 可选的地址标签 */
    private static final String[] ADDR_TAGS = { "家", "公司", "学校", "父母", "朋友", "自定义" };

    private LinearLayout llAddresses;
    private TextView tvEmpty;

    /** 当前打开的地址弹窗里的「详细地址」输入框，定位成功后把解析到的地址填进去 */
    private EditText currentAddressInput;
    /** 当前弹窗的「所在地区」输入框 */
    private EditText currentRegionInput;
    /** 当前弹窗定位卡片里的地址文字（展示完整解析结果） */
    private TextView currentLocationLabel;

    // 定位权限申请器：用户授权后立即开始获取位置
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if ((fine != null && fine) || (coarse != null && coarse)) {
                    fetchLocationAndFill();
                } else {
                    showToast("需要定位权限才能获取当前位置");
                }
            });

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
            TextView tagChip = item.findViewById(R.id.tvAddrTagChip);
            if (address.tag != null && !address.tag.isEmpty()) {
                tagChip.setText(address.tag);
                tagChip.setVisibility(View.VISIBLE);
            } else {
                tagChip.setVisibility(View.GONE);
            }
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
        EditText etRegion = view.findViewById(R.id.etRegion);
        Switch swDefault = view.findViewById(R.id.swDefaultAddress);
        LinearLayout llTags = view.findViewById(R.id.llAddrTags);
        TextView tvTitle = view.findViewById(R.id.tvAddrTitle);

        currentAddressInput = etAddress;
        currentRegionInput = etRegion;
        currentLocationLabel = view.findViewById(R.id.tvAddrLocation);

        final String[] selectedTag = { address != null ? address.tag : null };
        setupTagChips(llTags, selectedTag);

        view.findViewById(R.id.btnGetLocation).setOnClickListener(v -> onGetLocationClick());

        if (address != null) {
            tvTitle.setText("编辑地址");
            etName.setText(address.receiverName);
            etPhone.setText(address.phone);
            etAddress.setText(address.address);
            swDefault.setChecked(address.isDefault);
        } else {
            tvTitle.setText("新增地址");
            swDefault.setChecked(llAddresses.getChildCount() == 0);
        }

        // 全屏弹窗呈现（替代原居中小弹框，匹配整页式地址编辑体验）
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        view.findViewById(R.id.ivAddrBack).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSaveAddress).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String region = etRegion.getText().toString().trim();
            String detail = etAddress.getText().toString().trim();
            if (name.isEmpty() || phone.isEmpty() || detail.isEmpty()) {
                Toast.makeText(this, "请完整填写地址信息", Toast.LENGTH_SHORT).show();
                return;
            }
            String full = region.isEmpty() ? detail : region + " " + detail;
            presenter.saveAddress(address, name, phone, full, swDefault.isChecked(), selectedTag[0]);
            dialog.dismiss();
        });
        dialog.show();
    }

    // 构建地址标签胶囊：单选，再次点击同一个可取消选择
    private void setupTagChips(LinearLayout container, String[] selectedHolder) {
        container.removeAllViews();
        for (String tag : ADDR_TAGS) {
            TextView chip = new TextView(this);
            chip.setText(tag);
            chip.setTextSize(13);
            chip.setPadding(dp(18), dp(7), dp(18), dp(7));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(10);
            chip.setLayoutParams(lp);
            applyChipStyle(chip, tag.equals(selectedHolder[0]));
            chip.setOnClickListener(v -> {
                boolean select = !tag.equals(selectedHolder[0]);
                selectedHolder[0] = select ? tag : null;
                for (int i = 0; i < container.getChildCount(); i++) {
                    TextView c = (TextView) container.getChildAt(i);
                    applyChipStyle(c, c.getText().toString().equals(selectedHolder[0]));
                }
            });
            container.addView(chip);
        }
    }

    private void applyChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_addr_tag_selected : R.drawable.bg_addr_tag);
        chip.setTextColor(selected ? 0xFFFF7A1A : 0xFF555555);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    // 点击「获取本地位置」：有权限直接定位，否则先申请定位权限
    private void onGetLocationClick() {
        if (hasLocationPermission()) {
            fetchLocationAndFill();
        } else {
            locationPermissionLauncher.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION });
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // 取一次当前位置（优先网络定位，省电也更快），拿到经纬度后再做地址解析
    private void fetchLocationAndFill() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) {
            showToast("无法获取定位服务");
            return;
        }
        String provider = null;
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        }
        if (provider == null) {
            showToast("请先打开系统定位开关");
            return;
        }
        showToast("正在获取当前位置…");
        try {
            lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(this), location -> {
                if (location == null) {
                    showToast("定位失败，请到空旷处重试");
                } else {
                    reverseGeocode(location);
                }
            });
        } catch (SecurityException e) {
            showToast("缺少定位权限");
        }
    }

    // 把经纬度反向解析成中文地址（Geocoder 涉及网络，放到子线程）：
    // 「省市区」填入所在地区、「街道门牌」填入详细地址、完整地址显示在定位卡片上。
    private void reverseGeocode(Location location) {
        if (!Geocoder.isPresent()) {
            showToast("当前设备不支持地址解析");
            return;
        }
        new Thread(() -> {
            String region = null, detail = null, full = null;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.CHINA);
                List<android.location.Address> list = geocoder.getFromLocation(
                        location.getLatitude(), location.getLongitude(), 1);
                if (list != null && !list.isEmpty()) {
                    android.location.Address a = list.get(0);
                    region = formatRegion(a);
                    detail = formatDetail(a);
                    full = formatAddress(a);
                    if ((detail == null || detail.isEmpty()) && a.getMaxAddressLineIndex() >= 0) {
                        detail = a.getAddressLine(0);
                    }
                }
            } catch (IOException ignored) {
            }
            final String fRegion = region, fDetail = detail, fFull = full;
            runOnUiThread(() -> {
                if ((fRegion == null || fRegion.isEmpty()) && (fDetail == null || fDetail.isEmpty())) {
                    showToast("地址解析失败，请手动填写");
                    return;
                }
                if (currentRegionInput != null && fRegion != null && !fRegion.isEmpty()) {
                    currentRegionInput.setText(fRegion);
                }
                if (currentAddressInput != null && fDetail != null && !fDetail.isEmpty()) {
                    currentAddressInput.setText(fDetail);
                    currentAddressInput.setSelection(currentAddressInput.getText().length());
                }
                if (currentLocationLabel != null && fFull != null && !fFull.isEmpty()) {
                    currentLocationLabel.setText(fFull);
                }
                showToast("已填入当前位置");
            });
        }).start();
    }

    // 「省 + 市 + 区」用于所在地区
    private String formatRegion(android.location.Address a) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, a.getAdminArea());      // 省 / 直辖市
        appendPart(sb, a.getLocality());       // 市
        appendPart(sb, a.getSubLocality());    // 区 / 县
        return sb.toString();
    }

    // 「街道 + 门牌 + 地点名」用于详细地址
    private String formatDetail(android.location.Address a) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, a.getThoroughfare());    // 街道
        appendPart(sb, a.getSubThoroughfare()); // 门牌号
        String feature = a.getFeatureName();
        if (feature != null && !feature.equals(a.getThoroughfare()) && !feature.equals(a.getSubThoroughfare())) {
            appendPart(sb, feature);
        }
        return sb.toString();
    }

    // 完整地址（省市区街道门牌），用于定位卡片展示
    private String formatAddress(android.location.Address a) {
        String region = formatRegion(a);
        String detail = formatDetail(a);
        String combined = region + detail;
        if (combined.isEmpty() && a.getMaxAddressLineIndex() >= 0) {
            return a.getAddressLine(0);
        }
        return combined;
    }

    private void appendPart(StringBuilder sb, String part) {
        if (part != null && !part.isEmpty()) {
            sb.append(part);
        }
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
