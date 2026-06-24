package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import com.example.zhinongbao.mvp.addproductcomment.AddProductCommentContract;
import com.example.zhinongbao.mvp.addproductcomment.AddProductCommentPresenter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddProductCommentActivity extends BaseMvpActivity<AddProductCommentContract.Presenter>
        implements AddProductCommentContract.View {

    private int productId;
    private List<Uri> imageUris = new ArrayList<>();
    private ImagePickerAdapter adapter;
    private Uri currentCameraUri;

    private final ActivityResultLauncher<String> pickImages = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                        if (imageUris.size() < 9) {
                            imageUris.add(uri);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            });

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentCameraUri != null) {
                    if (imageUris.size() < 9) {
                        imageUris.add(currentCameraUri);
                        adapter.notifyDataSetChanged();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product_comment);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        productId = getIntent().getIntExtra("product_id", -1);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        EditText etContent = findViewById(R.id.etContent);
        TextView btnSubmit = findViewById(R.id.btnSubmitComment);
        RecyclerView rvCommentImages = findViewById(R.id.rvCommentImages);

        rvCommentImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new ImagePickerAdapter(imageUris, 9, new ImagePickerAdapter.OnImagePickerClickListener() {
            @Override
            public void onAddClick() {
                showImagePickerDialog();
            }

            @Override
            public void onDeleteClick(int position) {
                imageUris.remove(position);
                adapter.notifyDataSetChanged();
            }
        });
        rvCommentImages.setAdapter(adapter);

        new AddProductCommentPresenter(this, this, productId).start();
        if (!presenter.canComment()) {
            showReviewBlocked(presenter.getReviewBlockMessage());
            return;
        }

        btnSubmit.setOnClickListener(v -> {
            String content = etContent.getText().toString().trim();
            if (content.isEmpty() && imageUris.isEmpty()) {
                Toast.makeText(this, "评价内容和图片不能同时为空", Toast.LENGTH_SHORT).show();
                return;
            }
            String images = null;
            if (!imageUris.isEmpty()) {
                images = imageUris.stream().map(Uri::toString).collect(Collectors.joining(","));
            }
            presenter.submit(content, images);
        });
    }

    private void showReviewBlocked(String message) {
        findViewById(R.id.scrollReview).setVisibility(View.GONE);
        findViewById(R.id.btnSubmitComment).setVisibility(View.GONE);
        TextView notice = findViewById(R.id.tvReviewNotice);
        notice.setText(message);
        notice.setVisibility(View.VISIBLE);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }

    private Uri createImageFile() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists())
            imagePath.mkdirs();
        File newFile = new File(imagePath, "photo_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
    }

    private void showImagePickerDialog() {
        com.example.zhinongbao.utils.ImageUtils.showImagePickerDialog(this, "添加图片",
                new com.example.zhinongbao.utils.ImageUtils.OnImagePickerListener() {
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
}
