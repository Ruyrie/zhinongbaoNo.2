package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ImagePickerAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.addcirclepost.AddCirclePostContract;
import com.example.zhinongbao.mvp.addcirclepost.AddCirclePostPresenter;
import com.example.zhinongbao.utils.ImageUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddCirclePostActivity extends BaseMvpActivity<AddCirclePostContract.Presenter>
        implements AddCirclePostContract.View {

    private android.widget.EditText etContent;
    private ImagePickerAdapter imageAdapter;
    private TextView tvImageHint;
    private Uri selectedImageUri;
    private Uri currentCameraUri;
    private final List<Uri> selectedImageUris = new ArrayList<>();

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                    addImage(uri);
                }
            });

    private final ActivityResultLauncher<String> pickImages =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (selectedImageUris.size() >= 9) {
                            break;
                        }
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {}
                        addImage(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentCameraUri != null) {
                    addImage(currentCameraUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_circle_post);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        new AddCirclePostPresenter(this, this).start();

        etContent = findViewById(R.id.etCircleContent);
        tvImageHint = findViewById(R.id.tvImageHint);
        RecyclerView rvImages = findViewById(R.id.rvCircleImages);
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImagePickerAdapter(selectedImageUris, 9, new ImagePickerAdapter.OnImagePickerClickListener() {
            @Override
            public void onAddClick() {
                if (selectedImageUris.size() >= 9) {
                    Toast.makeText(AddCirclePostActivity.this, "最多上传 9 张照片", Toast.LENGTH_SHORT).show();
                    return;
                }
                pickImageSource();
            }

            @Override
            public void onDeleteClick(int position) {
                selectedImageUris.remove(position);
                selectedImageUri = selectedImageUris.isEmpty() ? null : selectedImageUris.get(0);
                imageAdapter.notifyDataSetChanged();
                updateImageHint();
            }
        });
        rvImages.setAdapter(imageAdapter);

        findViewById(R.id.tvCirclePostCancel).setOnClickListener(v -> finish());
        findViewById(R.id.tvCirclePostSubmit).setOnClickListener(v -> submit());
        findViewById(R.id.llAddImage).setOnClickListener(v -> pickImageSource());
    }

    private void pickImageSource() {
        ImageUtils.showImagePickerDialog(this, "选择图片",
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
        if (selectedImageUris.size() >= 9) {
            Toast.makeText(this, "最多上传 9 张照片", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedImageUri = uri;
        selectedImageUris.add(uri);
        imageAdapter.notifyDataSetChanged();
        updateImageHint();
    }

    private void updateImageHint() {
        int count = selectedImageUris.size();
        tvImageHint.setText(count == 0 ? "最多 9 张" : "已选择 " + count + " 张");
    }

    private Uri createImageFile() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists()) imagePath.mkdirs();
        File newFile = new File(imagePath, "circle_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
    }

    private void submit() {
        String content = etContent.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请输入动态内容", Toast.LENGTH_SHORT).show();
            return;
        }
        String imgUri = null;
        if (!selectedImageUris.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Uri uri : selectedImageUris) {
                if (sb.length() > 0)
                    sb.append(",");
                sb.append(uri.toString());
            }
            imgUri = sb.toString();
        } else if (selectedImageUri != null) {
            imgUri = selectedImageUri.toString();
        }
        presenter.submit(content, imgUri);
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
