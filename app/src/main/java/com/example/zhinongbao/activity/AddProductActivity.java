package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ImagePickerAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.mvp.addproduct.AddProductContract;
import com.example.zhinongbao.mvp.addproduct.AddProductPresenter;
import com.example.zhinongbao.utils.ImageUtils;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddProductActivity extends BaseMvpActivity<AddProductContract.Presenter> implements AddProductContract.View {

    private static final String[] CATEGORIES = { "推荐", "水果蔬菜", "米面粮油", "农资农具" };
    private Set<String> selectedCategories = new HashSet<>();
    private TextView[] categoryChips;

    private EditText etName, etDesc, etPrice, etStorePhone, etBrand, etOrigin, etSpec, etPackage;
    private boolean formattingPrice;
    private RecyclerView rvImages;
    private ImagePickerAdapter imageAdapter;
    private List<Uri> imageUris = new ArrayList<>();
    private Uri currentCameraUri;
    private int editProductId = -1;
    private String existingCoverUri;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    if (imageUris.size() < 9) {
                        imageUris.add(uri);
                        refreshImagePicker();
                    } else {
                        Toast.makeText(this, "最多只能添加9张图片", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentCameraUri != null) {
                    if (imageUris.size() < 9) {
                        imageUris.add(currentCameraUri);
                        refreshImagePicker();
                    } else {
                        Toast.makeText(this, "最多只能添加9张图片", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        etName = findViewById(R.id.etProductName);
        etDesc = findViewById(R.id.etProductDesc);
        etPrice = findViewById(R.id.etProductPrice);
        etStorePhone = findViewById(R.id.etStorePhone);
        etBrand = findViewById(R.id.etProductBrand);
        etOrigin = findViewById(R.id.etProductOrigin);
        etSpec = findViewById(R.id.etProductSpec);
        etPackage = findViewById(R.id.etProductPackage);
        bindPriceInput();
        rvImages = findViewById(R.id.rvProductImages);
        new AddProductPresenter(this, this).start();

        editProductId = getIntent().getIntExtra("product_id", -1);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitProduct).setOnClickListener(v -> submitProduct());

        // Setup image picker RecyclerView
        imageAdapter = new ImagePickerAdapter(imageUris, 9, new ImagePickerAdapter.OnImagePickerClickListener() {
            @Override
            public void onAddClick() {
                if (imageUris.size() >= 9) {
                    Toast.makeText(AddProductActivity.this, "最多只能添加9张图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                ImageUtils.showImagePickerDialog(AddProductActivity.this, "添加图片",
                        new ImageUtils.OnImagePickerListener() {
                            @Override
                            public void onTakePhoto() {
                                currentCameraUri = createImageFile();
                                takePicture.launch(currentCameraUri);
                            }

                            @Override
                            public void onPickFromGallery() {
                                pickImage.launch("image/*");
                            }
                        });
            }

            @Override
            public void onDeleteClick(int position) {
                if (position < 0 || position >= imageUris.size()) {
                    return;
                }
                imageUris.remove(position);
                refreshImagePicker();
            }
        });

        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(imageAdapter);

        // Default category
        selectedCategories.add("推荐");
        setupCategoryPicker();

        if (editProductId > 0) {
            TextView btnSubmit = findViewById(R.id.btnSubmitProduct);
            if (btnSubmit != null)
                btnSubmit.setText("保存修改");
            loadExistingProduct();
        }
    }

    private void loadExistingProduct() {
        presenter.loadProduct(editProductId);
    }

    @Override
    public void showStorePhone(String phone) {
        etStorePhone.setText(phone == null ? "" : phone);
    }

    @Override
    public void showExistingProduct(Product p) {
        if (p == null)
            return;
        etName.setText(p.name);
        etDesc.setText(p.desc);
        etPrice.setText(formatEditableNumber(formatPlain(p.price)));
        etBrand.setText(p.brand == null ? "" : p.brand);
        etOrigin.setText(p.origin == null ? "" : p.origin);
        etSpec.setText(p.spec == null ? "" : p.spec);
        etPackage.setText(p.packageType == null ? "" : p.packageType);
        existingCoverUri = p.coverUri;
        imageUris.clear();
        if (existingCoverUri != null && !existingCoverUri.isEmpty()) {
            for (String uri : existingCoverUri.split(",")) {
                String trimmed = uri.trim();
                if (!trimmed.isEmpty()) {
                    imageUris.add(Uri.parse(trimmed));
                }
            }
            if (imageAdapter != null)
                imageAdapter.notifyDataSetChanged();
        }
        selectedCategories.clear();
        if (p.category != null) {
            for (String c : p.category.split(",")) {
                String t = c.trim();
                if (!t.isEmpty())
                    selectedCategories.add(t);
            }
        }
        if (selectedCategories.isEmpty())
            selectedCategories.add("推荐");
        refreshCategoryChips();
    }

    @Override
    public void showToast(String message, boolean longToast) {
        Toast.makeText(this, message, longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }

    private void setupCategoryPicker() {
        LinearLayout container = findViewById(R.id.llCategoryPicker);
        if (container == null)
            return;
        categoryChips = new TextView[CATEGORIES.length];

        float density = getResources().getDisplayMetrics().density;
        int dp8 = (int) (8 * density);
        int dp14 = (int) (14 * density);
        int dp6 = (int) (6 * density);

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String cat = CATEGORIES[i];
            TextView chip = new TextView(this);
            chip.setText(cat);
            chip.setTextSize(13);
            chip.setPadding(dp14, dp6, dp14, dp6);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp8);
            chip.setLayoutParams(lp);

            applyCategoryChipStyle(chip, selectedCategories.contains(cat));
            chip.setOnClickListener(v -> {
                if (selectedCategories.contains(cat)) {
                    // Prevent unselecting all
                    if (selectedCategories.size() > 1) {
                        selectedCategories.remove(cat);
                    } else {
                        Toast.makeText(this, "至少选择一个分类", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    selectedCategories.add(cat);
                }
                refreshCategoryChips();
            });

            container.addView(chip);
            categoryChips[i] = chip;
        }
    }

    private void applyCategoryChipStyle(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_category_chip_selected);
            chip.setTextColor(0xFFFFFFFF);
            chip.setTypeface(null, Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_tag_chip);
            chip.setTextColor(0xFF3C3C43);
            chip.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void refreshCategoryChips() {
        if (categoryChips == null)
            return;
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (categoryChips[i] != null)
                applyCategoryChipStyle(categoryChips[i], selectedCategories.contains(CATEGORIES[i]));
        }
    }

    private void refreshImagePicker() {
        if (imageAdapter != null) {
            imageAdapter.notifyDataSetChanged();
        }
    }

    private Uri createImageFile() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists())
            imagePath.mkdirs();
        File newFile = new File(imagePath, "photo_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
    }

    private void bindPriceInput() {
        etPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!formattingPrice) {
                    formatPriceInput(s);
                }
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
            etPrice.setText(formatted);
            etPrice.setSelection(formatted.length());
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

    private String cleanNumber(String value) {
        return value == null ? "" : value.replace(",", "").replace("¥", "").trim();
    }

    private String formatPlain(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    private void submitProduct() {
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String priceStr = cleanNumber(etPrice.getText().toString().trim());
        String storePhone = etStorePhone.getText().toString().trim();
        String brand = etBrand.getText().toString().trim();
        String origin = etOrigin.getText().toString().trim();
        String spec = etSpec.getText().toString().trim();
        String packageType = etPackage.getText().toString().trim();

        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || storePhone.isEmpty()
                || brand.isEmpty() || origin.isEmpty() || spec.isEmpty() || packageType.isEmpty()) {
            Toast.makeText(this, "请完整填写商品信息", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, "请选择至少一个商品分类", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "商品价格无效", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder uriBuilder = new StringBuilder();
        for (int i = 0; i < imageUris.size(); i++) {
            uriBuilder.append(imageUris.get(i).toString());
            if (i < imageUris.size() - 1) {
                uriBuilder.append(",");
            }
        }

        // Build comma-separated categories
        StringBuilder catBuilder = new StringBuilder();
        List<String> list = new ArrayList<>(selectedCategories);
        for (int i = 0; i < list.size(); i++) {
            catBuilder.append(list.get(i));
            if (i < list.size() - 1) {
                catBuilder.append(",");
            }
        }

        String coverUri = uriBuilder.length() > 0 ? uriBuilder.toString() : null;
        if (editProductId > 0 && coverUri == null) {
            coverUri = existingCoverUri;
        }
        presenter.submitProduct(editProductId, name, desc, price, coverUri, catBuilder.toString(), storePhone,
                brand, origin, spec, packageType);
    }
}
