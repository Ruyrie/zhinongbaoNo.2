package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ImagePickerAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.mvp.postpurchase.PostPurchaseContract;
import com.example.zhinongbao.mvp.postpurchase.PostPurchasePresenter;
import com.example.zhinongbao.utils.ImageUtils;
import java.io.File;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ============================================================
 * 【发布采购需求 / PostPurchase】View（Activity 界面）
 * 整体逻辑：onCreate 绑定各输入框，配置图片选择（拍照/相册，最多 9 张），并在传入
 *   request_id 时把按钮文案改为「保存」并加载原需求回填；bindAmountInputs 监听数量与
 *   单价，实时格式化价格（千分位）并计算「预计总价」；submit 收集表单交给 Presenter，
 *   由 Presenter 校验后写库，成功则关闭本页。
 * 数据来源：PostPurchasePresenter 经 repository/PurchaseRepository 读写；
 *   Repository 内部经 ContentProvider 访问 SQLite。本类不直接碰数据库。
 * 配合的文件：接口约定 PostPurchaseContract；业务 PostPurchasePresenter；
 *   图片选择适配器 adapter/ImagePickerAdapter；数据模型 model/PurchaseRequest；
 *   布局 res/layout/activity_post_purchase.xml；工具 utils/ImageUtils；
 *   来源页面 PurchaseMarketActivity（发布/编辑入口）。
 * MVP 数据流位置：本类是 View；数据流为 View 到 Presenter 到 Repository 到
 *   ContentProvider 到 SQLite，再回调本类刷新界面。
 * 提示：在 IDE 里搜索「采购」可看本组相关文件。
 * ============================================================
 */
public class PostPurchaseActivity extends BaseMvpActivity<PostPurchaseContract.Presenter>
        implements PostPurchaseContract.View {

    private static final int MAX_IMAGES = 9;

    private EditText etProductName, etCategory, etQuantity, etUnit, etTargetPrice, etDesc;
    private TextView tvTotal, tvBudgetUnitLabel;
    private boolean formattingPrice;
    private long editRequestId = -1;

    // 需求图片选择
    private final List<Uri> imageUris = new ArrayList<>();
    private ImagePickerAdapter imageAdapter;
    private Uri currentCameraUri;

    private final ActivityResultLauncher<String> pickImages = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }
                for (Uri uri : uris) {
                    if (imageUris.size() >= MAX_IMAGES) {
                        break;
                    }
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                    }
                    addImage(uri);
                }
            });

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentCameraUri != null) {
                    addImage(currentCameraUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_purchase);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        new PostPurchasePresenter(this, this);
        editRequestId = getIntent().getLongExtra("request_id", -1);

        etProductName = findViewById(R.id.etPurchaseProductName);
        etCategory = findViewById(R.id.etPurchaseCategory);
        etQuantity = findViewById(R.id.etPurchaseQuantity);
        etUnit = findViewById(R.id.etPurchaseUnit);
        etTargetPrice = findViewById(R.id.etPurchaseTargetPrice);
        etDesc = findViewById(R.id.etPurchaseDesc);
        tvTotal = findViewById(R.id.tvPurchaseTotal);
        tvBudgetUnitLabel = findViewById(R.id.tvBudgetUnitLabel);
        bindAmountInputs();
        bindUnitLabel();
        setupImagePicker();

        findViewById(R.id.ivPostPurchaseBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitPurchase).setOnClickListener(v -> submit());
        if (editRequestId > 0) {
            ((TextView) findViewById(R.id.btnSubmitPurchase)).setText("保存");
            presenter.loadRequest(editRequestId);
        }
    }

    private void submit() {
        String name = etProductName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();
        String priceStr = cleanNumber(etTargetPrice.getText().toString().trim());
        String desc = etDesc.getText().toString().trim();
        String images = buildImageList();

        if (editRequestId > 0) {
            presenter.submitEdit(editRequestId, name, category, qtyStr, unit, priceStr, desc, images);
        } else {
            presenter.submit(name, category, qtyStr, unit, priceStr, desc, images);
        }
    }

    // ── 需求图片选择 ──

    private void setupImagePicker() {
        RecyclerView rv = findViewById(R.id.rvPurchaseImages);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImagePickerAdapter(imageUris, MAX_IMAGES,
                new ImagePickerAdapter.OnImagePickerClickListener() {
                    @Override
                    public void onAddClick() {
                        if (imageUris.size() >= MAX_IMAGES) {
                            showToast("最多上传 9 张图片");
                            return;
                        }
                        showImagePicker();
                    }

                    @Override
                    public void onDeleteClick(int position) {
                        imageUris.remove(position);
                        imageAdapter.notifyDataSetChanged();
                    }
                });
        rv.setAdapter(imageAdapter);
    }

    private void showImagePicker() {
        ImageUtils.showImagePickerDialog(this, "添加图片",
                new ImageUtils.OnImagePickerListener() {
                    @Override
                    public void onTakePhoto() {
                        currentCameraUri = createImageFile();
                        takePicture.launch(currentCameraUri);
                    }

                    @Override
                    public void onPickFromGallery() {
                        pickImages.launch("image/*");
                    }
                });
    }

    private void addImage(Uri uri) {
        if (imageUris.size() >= MAX_IMAGES) {
            showToast("最多上传 9 张图片");
            return;
        }
        imageUris.add(uri);
        imageAdapter.notifyDataSetChanged();
    }

    private Uri createImageFile() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists()) {
            imagePath.mkdirs();
        }
        File newFile = new File(imagePath, "photo_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
    }

    // 把已选图片拼成逗号分隔字符串存库（与订单 proof_images 同一格式）
    private String buildImageList() {
        StringBuilder sb = new StringBuilder();
        for (Uri uri : imageUris) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(uri.toString());
        }
        return sb.toString();
    }

    @Override
    public void showExistingRequest(PurchaseRequest request) {
        etProductName.setText(request.productName);
        etCategory.setText(request.category);
        etQuantity.setText(formatQty(request.quantity));
        etUnit.setText(request.unit);
        etTargetPrice.setText(formatEditableNumber(formatPlain(request.targetPrice)));
        etDesc.setText(request.description == null ? "" : request.description);
        imageUris.clear();
        if (!TextUtils.isEmpty(request.images)) {
            for (String uri : request.images.split(",")) {
                String trimmed = uri.trim();
                if (!trimmed.isEmpty() && imageUris.size() < MAX_IMAGES) {
                    imageUris.add(Uri.parse(trimmed));
                }
            }
        }
        if (imageAdapter != null) {
            imageAdapter.notifyDataSetChanged();
        }
        updateTotal();
    }

    private void bindUnitLabel() {
        etUnit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateUnitLabel();
            }
        });
        updateUnitLabel();
    }

    private void updateUnitLabel() {
        String unit = etUnit.getText().toString().trim();
        tvBudgetUnitLabel.setText("元/" + (unit.isEmpty() ? "单位" : unit));
    }

    private void bindAmountInputs() {
        TextWatcher totalWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateTotal();
            }
        };
        etQuantity.addTextChangedListener(totalWatcher);
        etTargetPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!formattingPrice) {
                    formatPriceInput(s);
                }
                updateTotal();
            }
        });
    }

    private void formatPriceInput(Editable s) {
        String raw = cleanNumber(s.toString());
        if (raw.isEmpty() || ".".equals(raw)) {
            return;
        }
        try {
            formattingPrice = true;
            String formatted = formatEditableNumber(raw);
            etTargetPrice.setText(formatted);
            etTargetPrice.setSelection(formatted.length());
        } finally {
            formattingPrice = false;
        }
    }

    private String formatEditableNumber(String raw) {
        int dotIndex = raw.indexOf('.');
        String integerPart = dotIndex >= 0 ? raw.substring(0, dotIndex) : raw;
        String decimalPart = dotIndex >= 0 ? raw.substring(dotIndex) : "";
        if (integerPart.isEmpty()) {
            return decimalPart.isEmpty() ? "" : "0" + decimalPart;
        }
        return groupInteger(integerPart) + decimalPart;
    }

    private String groupInteger(String value) {
        BigInteger integer = new BigInteger(value);
        String digits = integer.toString();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                builder.append(',');
            }
            builder.append(digits.charAt(i));
        }
        return builder.toString();
    }

    private void updateTotal() {
        try {
            double qty = Double.parseDouble(cleanNumber(etQuantity.getText().toString()));
            double price = Double.parseDouble(cleanNumber(etTargetPrice.getText().toString()));
            tvTotal.setText("¥" + formatMoney(qty * price));
        } catch (Exception e) {
            tvTotal.setText("¥0.00");
        }
    }

    private String cleanNumber(String value) {
        return value == null ? "" : value.replace(",", "").replace("¥", "").trim();
    }

    private String formatQty(double value) {
        return value % 1 == 0 ? String.valueOf((int) value) : String.valueOf(value);
    }

    private String formatPlain(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    private String formatMoney(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.CHINA);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }
}
