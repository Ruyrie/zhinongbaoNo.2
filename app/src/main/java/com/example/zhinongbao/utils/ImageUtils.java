package com.example.zhinongbao.utils;

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

    public interface OnImagePickerListener {
        void onTakePhoto();

        void onPickFromGallery();
    }

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

    public static String uriToBase64(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null)
                return null;

            // Compress and scale down to avoid large Base64 strings (Cursor window limit)
            int maxWidth = 500;
            int maxHeight = 500;
            float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            int width = Math.round((float) ratio * bitmap.getWidth());
            int height = Math.round((float) ratio * bitmap.getHeight());

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] bytes = baos.toByteArray();

            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setAvatarFromBase64(ImageView imageView, String base64Str) {
        if (base64Str == null || !base64Str.startsWith("data:image")) {
            return;
        }
        try {
            String cleanBase64 = base64Str.substring(base64Str.indexOf(",") + 1);
            byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
