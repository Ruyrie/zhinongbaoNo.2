package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.example.zhinongbao.mvp.addarticle.AddArticleContract;
import com.example.zhinongbao.mvp.addarticle.AddArticlePresenter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 【发文章 / Add Article】View（Activity）
 * 整体逻辑（关键步骤）：
 *   1) onCreate 初始化标题/正文输入框、封面选择区、内容配图横向列表、分类 chip、返回/提交按钮。
 *   2) 选图分两类：封面(pickCover 单选) 与 内容配图(pickContentImages 多选)，都支持拍照(takePicture)。
 *   3) buildImageList 把封面与内容图按约定格式拼成一个字符串（有封面则「封面URI\n内容图1,内容图2」，
 *      无封面则「内容图1,内容图2」，首张兼作封面），存入 cover_uri 字段。
 *   4) 提交时校验标题正文非空，调 presenter.submit(title, content, 图片串, 分类)。
 * 数据来源：本类不直接碰数据库，写入由 Presenter 调 ArticleRepository.addArticle
 *   完成（Repository 内部经 ContentProvider 写 SQLite）。
 * 配合的文件：接口 AddArticleContract；业务 AddArticlePresenter；
 *   选图适配器 adapter/ImagePickerAdapter；选图工具 utils/ImageUtils；
 *   布局 activity_add_article.xml；模型 model/Article（cover_uri 图片串格式见其注释）。
 * 在 MVP 数据流中的位置：View 层（View → Presenter → Repository → ContentProvider → SQLite）。
 * 提示：在 IDE 里搜索「发文章」可看本组相关文件。
 * ============================================================
 */
public class AddArticleActivity extends BaseMvpActivity<AddArticleContract.Presenter>
        implements AddArticleContract.View {

    private static final String[] CATEGORIES = { "热点新闻", "专家咨询", "支农宝新闻", "创业项目" };
    private static final int MAX_CONTENT_IMAGES = 9;

    private ImageView ivCover;
    private LinearLayout llCoverHint;
    private TextView tvContentImageHint;
    private EditText etTitle, etContent;

    private Uri coverUri;
    private final List<Uri> contentImageUris = new ArrayList<>();
    private ImagePickerAdapter contentImageAdapter;

    private Uri currentCameraUri;
    private boolean isPickingCoverForCamera = true;

    private String selectedCategory = CATEGORIES[0];
    private TextView[] categoryChips;

    private final ActivityResultLauncher<String> pickCover = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null)
                    return;
                coverUri = uri;
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                ivCover.setImageURI(uri);
                llCoverHint.setVisibility(View.GONE);
            });

    private final ActivityResultLauncher<String> pickContentImages = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris == null || uris.isEmpty())
                    return;
                for (Uri uri : uris) {
                    if (contentImageUris.size() >= MAX_CONTENT_IMAGES) {
                        break;
                    }
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                    }
                    addContentImage(uri);
                }
            });

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentCameraUri != null) {
                    if (isPickingCoverForCamera) {
                        coverUri = currentCameraUri;
                        ivCover.setImageURI(currentCameraUri);
                        llCoverHint.setVisibility(View.GONE);
                    } else {
                        addContentImage(currentCameraUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_article);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        new AddArticlePresenter(this, this).start();

        etTitle = findViewById(R.id.etArticleTitle);
        etContent = findViewById(R.id.etArticleContent);
        ivCover = findViewById(R.id.ivCover);
        llCoverHint = findViewById(R.id.llCoverHint);
        tvContentImageHint = findViewById(R.id.tvContentImageHint);
        FrameLayout flCover = findViewById(R.id.flCoverPicker);
        View tvBack = findViewById(R.id.tvBack);

        RecyclerView rvImages = findViewById(R.id.rvArticleImages);
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        contentImageAdapter = new ImagePickerAdapter(contentImageUris, MAX_CONTENT_IMAGES,
                new ImagePickerAdapter.OnImagePickerClickListener() {
                    @Override
                    public void onAddClick() {
                        if (contentImageUris.size() >= MAX_CONTENT_IMAGES) {
                            Toast.makeText(AddArticleActivity.this, "最多上传 9 张配图", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showContentImagePicker();
                    }

                    @Override
                    public void onDeleteClick(int position) {
                        contentImageUris.remove(position);
                        contentImageAdapter.notifyDataSetChanged();
                        updateContentImageHint();
                    }
                });
        rvImages.setAdapter(contentImageAdapter);

        flCover.setOnClickListener(v -> showImagePickerDialog(pickCover, true));
        tvBack.setOnClickListener(v -> finish());

        setupCategoryPicker();

        findViewById(R.id.btnSubmitArticle).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "标题和内容不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            presenter.submit(title, content, buildImageList(), selectedCategory);
        });
    }

    /**
     * 组装存入 cover_uri 的图片串（格式约定见 Article 类注释）：
     *   有专门封面：  "封面URI\n内容图1,内容图2,..."（换行分隔，详情页据此只显示内容配图）
     *   没有专门封面："内容图1,内容图2,..."（首张内容图兼作列表封面）
     *   都没有：       null
     */
    private String buildImageList() {
        StringBuilder content = new StringBuilder();
        for (Uri uri : contentImageUris) {
            if (content.length() > 0) {
                content.append(",");
            }
            content.append(uri.toString());
        }
        // 有专门封面：封面段 + 换行 + 内容段（内容段可能为空）
        if (coverUri != null) {
            return coverUri.toString() + "\n" + content;
        }
        // 没有专门封面：只存内容配图，首张兼作封面
        if (content.length() > 0) {
            return content.toString();
        }
        return null;
    }

    private void addContentImage(Uri uri) {
        if (contentImageUris.size() >= MAX_CONTENT_IMAGES) {
            Toast.makeText(this, "最多上传 9 张配图", Toast.LENGTH_SHORT).show();
            return;
        }
        contentImageUris.add(uri);
        contentImageAdapter.notifyDataSetChanged();
        updateContentImageHint();
    }

    private void updateContentImageHint() {
        int count = contentImageUris.size();
        tvContentImageHint.setText(count == 0
                ? "内容配图（最多 9 张）"
                : "内容配图（已选 " + count + "/9 张）");
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

            applyCategoryChipStyle(chip, cat.equals(selectedCategory));
            chip.setOnClickListener(v -> {
                selectedCategory = cat;
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
            applyCategoryChipStyle(categoryChips[i], CATEGORIES[i].equals(selectedCategory));
        }
    }

    private Uri createImageFile() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists())
            imagePath.mkdirs();
        File newFile = new File(imagePath, "photo_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
    }

    /** 封面图选择（单图） */
    private void showImagePickerDialog(ActivityResultLauncher<String> galleryLauncher, boolean isCover) {
        com.example.zhinongbao.utils.ImageUtils.showImagePickerDialog(this, "添加图片",
                new com.example.zhinongbao.utils.ImageUtils.OnImagePickerListener() {
                    @Override
                    public void onTakePhoto() {
                        isPickingCoverForCamera = isCover;
                        currentCameraUri = createImageFile();
                        takePicture.launch(currentCameraUri);
                    }

                    @Override
                    public void onPickFromGallery() {
                        galleryLauncher.launch("image/*");
                    }
                });
    }

    /** 内容配图选择（多图，最多 9 张） */
    private void showContentImagePicker() {
        com.example.zhinongbao.utils.ImageUtils.showImagePickerDialog(this, "添加配图",
                new com.example.zhinongbao.utils.ImageUtils.OnImagePickerListener() {
                    @Override
                    public void onTakePhoto() {
                        isPickingCoverForCamera = false;
                        currentCameraUri = createImageFile();
                        takePicture.launch(currentCameraUri);
                    }

                    @Override
                    public void onPickFromGallery() {
                        pickContentImages.launch("image/*");
                    }
                });
    }
}
