package com.example.zhinongbao.adapter;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.provider.MediaStore;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.ChatMessage;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 0;
    private static final int TYPE_RECEIVED = 1;

    private final List<ChatMessage> messages;
    private final String currentUser;
    private final String currentNickname;
    private final String otherNickname;

    public ChatAdapter(List<ChatMessage> messages, String currentUser, String currentNickname, String otherNickname) {
        this.messages = messages;
        this.currentUser = currentUser;
        this.currentNickname = currentNickname != null && !currentNickname.isEmpty() ? currentNickname : currentUser;
        this.otherNickname = otherNickname != null && !otherNickname.isEmpty() ? otherNickname : "?";
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).fromUser.equals(currentUser) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            View v = inf.inflate(R.layout.item_chat_sent, parent, false);
            return new SentHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        String timeStr = formatTime(msg.timestamp);
        if (holder instanceof SentHolder) {
            SentHolder h = (SentHolder) holder;
            h.tvAvatar.setText(currentNickname.substring(0, 1).toUpperCase());
            h.tvNickname.setText(currentNickname);
            bindMessageContent(h.tvContent, h.ivImage, msg);
            h.tvTime.setText(timeStr);
        } else {
            ReceivedHolder h = (ReceivedHolder) holder;
            h.tvAvatar.setText(otherNickname.substring(0, 1).toUpperCase());
            h.tvNickname.setText(otherNickname);
            bindMessageContent(h.tvContent, h.ivImage, msg);
            h.tvTime.setText(timeStr);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvNickname, tvContent, tvTime;
        ImageView ivImage;
        SentHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvSentAvatar);
            tvNickname = v.findViewById(R.id.tvSentNickname);
            tvContent = v.findViewById(R.id.tvSentContent);
            tvTime = v.findViewById(R.id.tvSentTime);
            ivImage = v.findViewById(R.id.ivSentImage);
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvNickname, tvContent, tvTime;
        ImageView ivImage;
        ReceivedHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvReceivedAvatar);
            tvNickname = v.findViewById(R.id.tvReceivedNickname);
            tvContent = v.findViewById(R.id.tvReceivedContent);
            tvTime = v.findViewById(R.id.tvReceivedTime);
            ivImage = v.findViewById(R.id.ivReceivedImage);
        }
    }

    private void bindMessageContent(TextView tvContent, ImageView ivImage, ChatMessage msg) {
        if (msg.isImage()) {
            tvContent.setVisibility(View.GONE);
            ivImage.setVisibility(View.VISIBLE);
            String imageUri = ChatMessage.imageUri(msg.content);
            try {
                ivImage.setImageURI(Uri.parse(imageUri));
            } catch (Exception e) {
                ivImage.setImageResource(R.drawable.ic_product_placeholder);
            }
            ivImage.setOnClickListener(v -> showImagePreview(ivImage, imageUri));
        } else {
            ivImage.setVisibility(View.GONE);
            ivImage.setOnClickListener(null);
            tvContent.setVisibility(View.VISIBLE);
            tvContent.setText(msg.content);
        }
    }

    private void showImagePreview(ImageView source, String imageUri) {
        if (imageUri == null || imageUri.isEmpty()) {
            return;
        }
        Dialog dialog = new Dialog(source.getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout root = new FrameLayout(source.getContext());
        root.setBackgroundColor(Color.BLACK);

        ImageView preview = new ImageView(source.getContext());
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        try {
            preview.setImageURI(Uri.parse(imageUri));
        } catch (Exception e) {
            preview.setImageResource(R.drawable.ic_product_placeholder);
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        root.addView(preview, lp);

        ImageView download = new ImageView(source.getContext());
        download.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        download.setPadding(dp(source, 11), dp(source, 11), dp(source, 11), dp(source, 11));
        GradientDrawable downloadBg = new GradientDrawable();
        downloadBg.setColor(0x66000000);
        downloadBg.setShape(GradientDrawable.OVAL);
        download.setBackground(downloadBg);
        try (InputStream is = source.getContext().getAssets().open("pic/fangda.png")) {
            download.setImageBitmap(BitmapFactory.decodeStream(is));
        } catch (Exception ignored) {
            download.setImageResource(android.R.drawable.stat_sys_download_done);
        }
        FrameLayout.LayoutParams downloadLp = new FrameLayout.LayoutParams(dp(source, 46), dp(source, 46));
        downloadLp.gravity = Gravity.TOP | Gravity.END;
        downloadLp.topMargin = dp(source, 28);
        downloadLp.rightMargin = dp(source, 18);
        root.addView(download, downloadLp);

        root.setOnClickListener(v -> dialog.dismiss());
        preview.setOnClickListener(v -> dialog.dismiss());
        download.setOnClickListener(v -> saveImageToGallery(source, imageUri));

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        });
        dialog.show();
    }

    private void saveImageToGallery(ImageView source, String imageUri) {
        ContentResolver resolver = source.getContext().getContentResolver();
        String filename = "zhinongbao_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/支农宝");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri targetUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (targetUri == null) {
            Toast.makeText(source.getContext(), "保存失败", Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream is = resolver.openInputStream(Uri.parse(imageUri));
             OutputStream os = resolver.openOutputStream(targetUri)) {
            if (is == null || os == null) {
                throw new IllegalStateException("image stream is empty");
            }
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(targetUri, values, null, null);
            Toast.makeText(source.getContext(), "已保存到本地相册", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            resolver.delete(targetUri, null, null);
            Toast.makeText(source.getContext(), "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
    }

    private String formatTime(long ts) {
        Calendar today = Calendar.getInstance();
        Calendar msgDay = Calendar.getInstance();
        msgDay.setTimeInMillis(ts);

        boolean sameDay = today.get(Calendar.YEAR) == msgDay.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == msgDay.get(Calendar.DAY_OF_YEAR);

        if (sameDay) {
            return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ts));
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        boolean isYesterday = yesterday.get(Calendar.YEAR) == msgDay.get(Calendar.YEAR)
                && yesterday.get(Calendar.DAY_OF_YEAR) == msgDay.get(Calendar.DAY_OF_YEAR);

        if (isYesterday) {
            return "昨天 " + new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ts));
        }

        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(ts));
    }
}
