package com.example.zhinongbao;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ImagePickerAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.utils.ImageUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddProductActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = { "推荐", "水果蔬菜", "米面粮油", "农资农具" };
    private Set<String> selectedCategories = new HashSet<>();
    private TextView[] categoryChips;

    private EditText etName, etDesc, etPrice, etStorePhone;
    private RecyclerView rvImages;
    private ImagePickerAdapter imageAdapter;
    private List<Uri> imageUris = new ArrayList<>();
    private Uri currentCameraUri;
    private int editProductId = -1;

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
                        imageAdapter.notifyItemInserted(imageUris.size() - 1);
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
                        imageAdapter.notifyItemInserted(imageUris.size() - 1);
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
        rvImages = findViewById(R.id.rvProductImages);
        String user = DataManager.getInstance(this).getLoggedUser();
        if (user != null) {
            etStorePhone.setText(DataManager.getInstance(this).getStorePhone(user));
        }

        editProductId = getIntent().getIntExtra("product_id", -1);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitProduct).setOnClickListener(v -> submitProduct());

        if (editProductId > 0) {
            TextView btnSubmit = findViewById(R.id.btnSubmitProduct);
            if (btnSubmit != null)
                btnSubmit.setText("保存修改");
            loadExistingProduct();
        }

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
                imageUris.remove(position);
                imageAdapter.notifyItemRemoved(position);
            }
        });

        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(imageAdapter);

        // Default category
        selectedCategories.add("推荐");
        setupCategoryPicker();
    }

    private void loadExistingProduct() {
        DataManager dm = DataManager.getInstance(this);
        com.example.zhinongbao.model.Product p = dm.getProductById(editProductId);
        if (p == null)
            return;
        etName.setText(p.name);
        etDesc.setText(p.desc);
        etPrice.setText(String.valueOf(p.price));
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
        for (int i = 0; i < CATEGORIES.length; i++) {
            applyCategoryChipStyle(categoryChips[i], selectedCategories.contains(CATEGORIES[i]));
        }
    }

    private Uri createImageFile() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists())
            imagePath.mkdirs();
        File newFile = new File(imagePath, "photo_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
    }

    private void submitProduct() {
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String storePhone = etStorePhone.getText().toString().trim();

        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || storePhone.isEmpty()) {
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

        DataManager dm = DataManager.getInstance(this);
        String coverUri = uriBuilder.length() > 0 ? uriBuilder.toString() : null;
        String user = dm.getLoggedUser();
        if (user != null) {
            dm.updateStoreInfo(user, dm.getStoreName(user), storePhone);
        }

        if (editProductId > 0) {
            dm.updateProduct(editProductId, name, desc, price, coverUri, catBuilder.toString());
            Toast.makeText(this, "货品信息已更新", Toast.LENGTH_SHORT).show();
        } else {
            dm.addProduct(name, desc, price, coverUri != null ? coverUri : "", catBuilder.toString());
            if (user != null && dm.getUserRole(user) == com.example.zhinongbao.model.User.ROLE_BUYER) {
                dm.updateUserRole(user, com.example.zhinongbao.model.User.ROLE_BOTH);
                Toast.makeText(this, "商品发布成功！您已获得卖家身份，下次登录可选择身份", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "商品发布成功", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}
