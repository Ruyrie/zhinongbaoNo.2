package com.example.zhinongbao;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.profileedit.ProfileEditContract;
import com.example.zhinongbao.mvp.profileedit.ProfileEditPresenter;
import java.io.File;

public class ProfileEditActivity extends BaseMvpActivity<ProfileEditContract.Presenter>
        implements ProfileEditContract.View {

    private String pendingAvatarUri;
    private String pendingAvatarBase64;
    private TextView tvPhone;
    private TextView tvBindPhoneBtn;
    private EditText etNickname;
    private EditText etSignature;
    private ImageView ivAvatar;

    private Uri currentCameraUri;

    private final ActivityResultLauncher<Intent> uCropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri resultUri = com.yalantis.ucrop.UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        processDirectImage(resultUri);
                    }
                } else if (result.getResultCode() == com.yalantis.ucrop.UCrop.RESULT_ERROR
                        && result.getData() != null) {
                    Throwable cropError = com.yalantis.ucrop.UCrop.getError(result.getData());
                    if (cropError != null)
                        android.util.Log.e("ProfileEdit", "裁剪出错", cropError);
                    Toast.makeText(this, "裁剪出错", Toast.LENGTH_SHORT).show();
                }
            });

    private void processDirectImage(Uri uri) {
        pendingAvatarUri = uri.toString();
        pendingAvatarBase64 = com.example.zhinongbao.utils.ImageUtils.uriToBase64(this, uri);
        ImageView iv = findViewById(R.id.ivAvatarPreview);
        if (pendingAvatarBase64 != null) {
            com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(iv, pendingAvatarBase64);
        } else {
            iv.setImageURI(uri);
        }
        iv.setBackground(null);
    }

    private void startCrop(Uri sourceUri) {
        try {
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "crop_" + System.currentTimeMillis() + ".jpg"));
            com.yalantis.ucrop.UCrop uCrop = com.yalantis.ucrop.UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(500, 500); // 提高清晰度

            com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
            options.setCircleDimmedLayer(true); // 显示圆形遮罩
            options.setShowCropGrid(false);
            options.setHideBottomControls(false); // 允许缩放和旋转
            uCrop.withOptions(options);

            Intent intent = uCrop.getIntent(this);
            intent.setClass(this, UCropCompatActivity.class);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            uCropLauncher.launch(intent);
        } catch (Exception e) {
            android.util.Log.e("ProfileEdit", "启动裁剪失败", e);
            Toast.makeText(this, "启动裁剪失败", Toast.LENGTH_SHORT).show();
            processDirectImage(sourceUri);
        }
    }

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            });

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentCameraUri != null) {
                    startCrop(currentCameraUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        etNickname = findViewById(R.id.etNickname);
        etSignature = findViewById(R.id.etSignature);
        ivAvatar = findViewById(R.id.ivAvatarPreview);
        ImageView tvBack = findViewById(R.id.tvBack);
        TextView tvSave = findViewById(R.id.tvSave);
        tvPhone = findViewById(R.id.tvPhone);
        tvBindPhoneBtn = findViewById(R.id.tvBindPhoneBtn);

        new ProfileEditPresenter(this, this).start();

        tvBack.setOnClickListener(v -> finish());

        tvSave.setOnClickListener(v -> {
            String nick = etNickname.getText().toString().trim();
            String sig = etSignature.getText().toString().trim();
            String avatar = pendingAvatarBase64 != null ? pendingAvatarBase64 : pendingAvatarUri;
            presenter.saveProfile(nick, sig, avatar);
        });

        findViewById(R.id.layoutAvatar).setOnClickListener(v -> showAvatarPreviewDialog());

        tvBindPhoneBtn.setOnClickListener(v -> showBindPhoneDialog());
    }

    private void showAvatarPreviewDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_avatar_preview);

        ImageView ivPreview = dialog.findViewById(R.id.ivFullscreenAvatar);
        // 加载当前显示的头像（与主界面一致）
        String currentUri = pendingAvatarBase64 != null ? pendingAvatarBase64
                : (pendingAvatarUri != null ? pendingAvatarUri : presenter.getCurrentAvatarUri());

        if (currentUri != null) {
            try {
                if (currentUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(ivPreview, currentUri);
                } else {
                    ivPreview.setImageURI(Uri.parse(currentUri));
                }
            } catch (Exception e) {
                ivPreview.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            ivPreview.setImageResource(R.mipmap.ic_launcher);
        }

        dialog.findViewById(R.id.btnChangeAvatar).setOnClickListener(v -> {
            dialog.dismiss();
            showImagePickerDialog();
        });

        dialog.findViewById(R.id.ivClosePreview).setOnClickListener(v -> dialog.dismiss());
        ivPreview.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private Uri createImageFile() {
        File imagePath = new File(getCacheDir(), "images");
        if (!imagePath.exists() && !imagePath.mkdirs())
            android.util.Log.w("ProfileEdit", "Failed to create image cache dir");
        File newFile = new File(imagePath, "avatar_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
    }

    private void showImagePickerDialog() {
        com.example.zhinongbao.utils.ImageUtils.showImagePickerDialog(this, "修改头像",
                new com.example.zhinongbao.utils.ImageUtils.OnImagePickerListener() {
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
    public void showProfile(String nickname, String signature, String avatarUri, String phone) {
        etNickname.setText(nickname);
        etSignature.setText(signature);
        if (avatarUri != null) {
            try {
                if (avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(ivAvatar, avatarUri);
                } else {
                    ivAvatar.setImageURI(Uri.parse(avatarUri));
                }
                ivAvatar.setBackground(null);
            } catch (Exception ignored) {
            }
        }
        showPhone(phone);
    }

    @Override
    public void showPhone(String phone) {
        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
            tvBindPhoneBtn.setText("修改绑定");
        } else {
            tvPhone.setText("未绑定");
            tvBindPhoneBtn.setText("去绑定");
        }
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }

    private void showBindPhoneDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_bind_phone, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText input = view.findViewById(R.id.etBindPhoneInput);
        view.findViewById(R.id.btnBindCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnBindConfirm).setOnClickListener(v -> {
            String phone = input.getText().toString().trim();
            if (phone.isEmpty()) {
                Toast.makeText(this, "手机号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.length() != 11 || !phone.matches("^1[3-9]\\d{9}$") || phone.matches("^(\\d)\\1{10}$")) {
                Toast.makeText(this, "请输入有效的11位手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            presenter.bindPhone(phone);
            dialog.dismiss();
        });
        dialog.show();
    }
}
