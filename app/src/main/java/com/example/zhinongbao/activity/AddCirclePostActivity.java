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

/**
 * ============================================================
 * 【发农友圈动态 / Add Circle Post】View（Activity）
 * 整体逻辑（关键步骤）：
 *   1) onCreate 初始化输入框、图片横向列表(ImagePickerAdapter)、取消/发布按钮；
 *      读取 intent 里的 edit_post_id 判断是「发布新动态」还是「编辑已有动态」。
 *   2) 点「加号」弹选图对话框：拍照走 takePicture（先用 FileProvider 建临时文件），
 *      相册走 pickImages（多选）；选中的图 addImage 加入列表并更新数量提示。
 *   3) 编辑模式：presenter.loadPost 取回原动态，showExistingPost 把正文与图片回填到表单；
 *      标题改「编辑动态」、按钮改「保存」。
 *   4) submit 校验文字非空，把多张图 URI 用逗号拼成一个字符串：新建走 presenter.submit，
 *      编辑走 presenter.submitEdit(editPostId, content, imgUri)。
 * 数据来源：本类不直接碰数据库，写入/更新由 Presenter 调 ArticleRepository.addCirclePost /
 *   updateCirclePost 完成（Repository 内部经 ContentProvider 写 SQLite）。
 * 配合的文件：接口 AddCirclePostContract；业务 AddCirclePostPresenter；
 *   选图适配器 adapter/ImagePickerAdapter；选图工具 utils/ImageUtils；
 *   布局 activity_add_circle_post.xml；发布成功后返回农友圈列表。
 * 在 MVP 数据流中的位置：View 层（View → Presenter → Repository → ContentProvider → SQLite）。
 * 提示：在 IDE 里搜索「发农友圈」可看本组相关文件。
 * ============================================================
 */
public class AddCirclePostActivity extends BaseMvpActivity<AddCirclePostContract.Presenter>
        implements AddCirclePostContract.View {

    private android.widget.EditText etContent;
    private ImagePickerAdapter imageAdapter;
    private TextView tvImageHint;
    private Uri selectedImageUri;
    private Uri currentCameraUri;
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private int editPostId = -1;   // >0 表示「编辑已有动态」，否则为「发布新动态」

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

        // 编辑模式：改标题/按钮文案，并让 Presenter 取回原动态回填表单
        editPostId = getIntent().getIntExtra("edit_post_id", -1);
        if (editPostId > 0) {
            ((TextView) findViewById(R.id.tvCirclePostTitle)).setText("编辑动态");
            ((TextView) findViewById(R.id.tvCirclePostSubmit)).setText("保存");
            presenter.loadPost(editPostId);
        }
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
        if (editPostId > 0) {
            presenter.submitEdit(editPostId, content, imgUri); // 编辑模式：保存修改
        } else {
            presenter.submit(content, imgUri);                 // 新建模式：发布新动态
        }
    }

    // 编辑模式回填：把原动态的正文与图片填回输入框和图片列表
    @Override
    public void showExistingPost(String content, String imageUris) {
        if (content != null) {
            etContent.setText(content);
            etContent.setSelection(etContent.getText().length()); // 光标移到末尾
        }
        selectedImageUris.clear();
        if (imageUris != null && !imageUris.isEmpty()) {
            for (String uri : imageUris.split(",")) {
                String trimmed = uri.trim();
                if (!trimmed.isEmpty()) {
                    selectedImageUris.add(Uri.parse(trimmed));
                }
            }
        }
        selectedImageUri = selectedImageUris.isEmpty() ? null : selectedImageUris.get(0);
        imageAdapter.notifyDataSetChanged();
        updateImageHint();
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
