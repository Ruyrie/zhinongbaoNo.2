package com.example.zhinongbao.utils;

/* ============================================================
 * 【图片 / 头像 / ImageUtils】图片相关工具（utils 工具类）
 * ============================================================
 * 这个文件是干什么的：本项目里凡是「选图片、存图片、显示图片」都用到它，包含三件事：
 *   1) showImagePickerDialog：从底部弹出「拍照 / 从相册选」的选择框。
 *   2) uriToBase64：把手机里的图片压缩缩小后，转成一长串 Base64 文本，方便存进数据库。
 *   3) setAvatarFromBase64：把数据库里那串 Base64 文本还原成图片，显示到 ImageView。
 *
 * 概念说明：
 *   - Uri：图片在手机里的「地址」。
 *   - Bitmap：内存里的一张图片。
 *   - Base64：把图片的二进制数据编码成纯文本字符串（这样能直接当文字存数据库）。
 *     存之前先缩到最大 500×500 并压缩，避免字符串太长撑爆数据库读取限制。
 *   - static 方法：不用 new 对象，直接 ImageUtils.xxx() 调用。
 *
 * 提示：在 IDE 里搜索「图片」或「头像」可看用到它的页面（资料编辑/发布/评价/聊天等）。
 * ============================================================ */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.zhinongbao.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    // 选图回调接口：调用方实现这两个方法，决定用户点「拍照」或「相册」后各做什么
    public interface OnImagePickerListener {
        void onTakePhoto();        // 用户选择了「拍照」

        void onPickFromGallery();  // 用户选择了「从相册选」
    }

    // 弹出底部选择框：拍照 / 从相册选 / 取消
    public static void showImagePickerDialog(Context context, String title, OnImagePickerListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_image_picker, null);

        TextView tvTitle = view.findViewById(R.id.tvPickerTitle);
        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
        }

        view.findViewById(R.id.btnTake_photo).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onTakePhoto();
        });

        view.findViewById(R.id.btnOpen_gallery).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onPickFromGallery();
        });

        view.findViewById(R.id.btnPickerCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    // 把图片地址(Uri) → 压缩缩小 → 转成 Base64 文本（失败返回 null）
    public static String uriToBase64(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri); // 打开图片
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);                     // 读成 Bitmap
            if (bitmap == null)
                return null;

            // 缩小到最大 500×500：避免 Base64 字符串过长，超出数据库一次读取的大小限制
            int maxWidth = 500;
            int maxHeight = 500;
            // 等比例缩放：算出宽高各按多大比例缩，保证图片不变形
            float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            int width = Math.round((float) ratio * bitmap.getWidth());
            int height = Math.round((float) ratio * bitmap.getHeight());

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);   // 压成 JPEG，质量 85
            byte[] bytes = baos.toByteArray();

            // 加上 data:image/jpeg;base64, 前缀，方便之后识别这是一段图片文本
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 把数据库里那串 Base64 图片文本 → 还原成图片 → 显示到 ImageView
    public static void setAvatarFromBase64(ImageView imageView, String base64Str) {
        if (base64Str == null || !base64Str.startsWith("data:image")) {
            return;   // 不是图片文本就什么都不做
        }
        try {
            String cleanBase64 = base64Str.substring(base64Str.indexOf(",") + 1); // 去掉前缀只留正文
            byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);    // 文本解码回二进制
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length); // 二进制转图片
            imageView.setImageBitmap(decodedByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
