package com.example.zhinongbao;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.utils.ImageUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddCirclePostActivity extends AppCompatActivity {

    private android.widget.EditText etContent;
    private ImageView ivPreview;
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
                    selectedImageUri = uri;
                    selectedImageUris.clear();
                    selectedImageUris.add(uri);
                    showPreview(uri);
                }
            });

    private final ActivityResultLauncher<String> pickImages =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    selectedImageUris.clear();
                    for (Uri uri : uris) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {}
                        selectedImageUris.add(uri);
                    }
                    selectedImageUri = selectedImageUris.get(0);
                    showPreview(selectedImageUri);
                }
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentCameraUri != null) {
                    selectedImageUri = currentCameraUri;
                    selectedImageUris.clear();
                    selectedImageUris.add(currentCameraUri);
                    showPreview(currentCameraUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_circle_post);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        etContent = findViewById(R.id.etCircleContent);
        ivPreview = findViewById(R.id.ivCircleSelectedImage);
        tvImageHint = findViewById(R.id.tvImageHint);

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

    private void showPreview(Uri uri) {
        ivPreview.setVisibility(android.view.View.VISIBLE);
        ivPreview.setImageURI(uri);
        int count = selectedImageUris.isEmpty() ? 1 : selectedImageUris.size();
        tvImageHint.setText("已选择 " + count + " 张");
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
        DataManager.getInstance(this).addCirclePost(content, imgUri);
        Toast.makeText(this, "发布成功！", Toast.LENGTH_SHORT).show();
        finish();
    }
}
