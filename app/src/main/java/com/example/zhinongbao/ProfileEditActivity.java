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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.zhinongbao.data.DataManager;
import java.io.File;

public class ProfileEditActivity extends AppCompatActivity {

    private DataManager dm;
    private String username;
    private String pendingAvatarUri;
    private String pendingAvatarBase64;
    private TextView tvPhone;
    private TextView tvBindPhoneBtn;

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

        dm = DataManager.getInstance(this);
        username = dm.getLoggedUser();

        EditText etNickname = findViewById(R.id.etNickname);
        EditText etSignature = findViewById(R.id.etSignature);
        ImageView ivAvatar = findViewById(R.id.ivAvatarPreview);
        ImageView tvBack = findViewById(R.id.tvBack);
        TextView tvSave = findViewById(R.id.tvSave);
        tvPhone = findViewById(R.id.tvPhone);
        tvBindPhoneBtn = findViewById(R.id.tvBindPhoneBtn);

        etNickname.setText(dm.getNickname(username));
        etSignature.setText(dm.getSignature(username));

        refreshPhoneDisplay();

        String existingUri = dm.getAvatarUri(username);
        if (existingUri != null) {
            try {
                if (existingUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(ivAvatar, existingUri);
                } else {
                    ivAvatar.setImageURI(Uri.parse(existingUri));
                }
                ivAvatar.setBackground(null);
            } catch (Exception ignored) {
            }
        }

        tvBack.setOnClickListener(v -> finish());

        tvSave.setOnClickListener(v -> {
            String nick = etNickname.getText().toString().trim();
            String sig = etSignature.getText().toString().trim();
            if (nick.isEmpty()) {
                Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (nick.length() > 12) {
                Toast.makeText(this, "昵称不能超过12个字符", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = true;

            // 只有发生了变化，或者用户上传了新头像，才去更新那单独的一项
            if (!nick.equals(dm.getNickname(username))) {
                success &= dm.setNickname(username, nick);
            }
            if (!sig.equals(dm.getSignature(username))) {
                success &= dm.updateSignature(username, sig);
            }
            if (pendingAvatarBase64 != null) {
                success &= dm.setAvatarUri(username, pendingAvatarBase64);
            } else if (pendingAvatarUri != null) {
                success &= dm.setAvatarUri(username, pendingAvatarUri);
            }

            if (success) {
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "保存失败，该账号状态异常，请重新登录", Toast.LENGTH_LONG).show();
            }
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
                : (pendingAvatarUri != null ? pendingAvatarUri : dm.getAvatarUri(username));

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

    private void refreshPhoneDisplay() {
        String phone = dm.getPhone(username);
        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
            tvBindPhoneBtn.setText("修改绑定");
        } else {
            tvPhone.setText("未绑定");
            tvBindPhoneBtn.setText("去绑定");
        }
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
            boolean ok = dm.updatePhone(username, phone);
            if (ok) {
                Toast.makeText(this, "绑定成功", Toast.LENGTH_SHORT).show();
                refreshPhoneDisplay();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "该手机号已被其他账号绑定，请更换手机号", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
}
