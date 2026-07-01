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

/**
 * ============================================================
 * 【发表商品评价 / Add Product Comment】View（界面/Activity）
 * 整体逻辑：onCreate 先创建 Presenter 并校验 canComment()——没买过或未确认收货
 *   则显示拦截提示、隐藏输入区；可评价时点提交会校验内容与图片不能同时为空，
 *   把图片 Uri 拼成逗号分隔字符串交给 presenter.submit 发布。图片选择用
 *   ActivityResultLauncher（拍照 TakePicture / 相册 GetMultipleContents），
 *   选好后加入 imageUris 并刷新 ImagePickerAdapter。
 * 数据来源：不直接碰数据库；经 Presenter 走 repository/ProductRepository，
 *   Repository 内部通过 ContentProvider 访问 SQLite。
 * 配合的文件：接口约定 mvp/addproductcomment/AddProductCommentContract；业务逻辑
 *   AddProductCommentPresenter；图片选择适配器 adapter/ImagePickerAdapter；
 *   图片工具 utils/ImageUtils；页面布局 res/layout/activity_add_product_comment.xml。
 * 在 MVP 数据流中的位置：View 层。
 * 提示：在 IDE 里搜索「发表评价」可看本组相关文件。
 * ============================================================
 */
public class AddProductCommentActivity extends BaseMvpActivity<AddProductCommentContract.Presenter>
        implements AddProductCommentContract.View {

    private int productId;                          // 要评价的商品 id
    private List<Uri> imageUris = new ArrayList<>(); // 已选择的图片，最多 9 张
    private ImagePickerAdapter adapter;             // 图片选择/预览适配器
    private Uri currentCameraUri;                   // 本次拍照写入的临时文件 Uri

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
