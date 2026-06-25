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
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.ChatMessage;
import com.example.zhinongbao.mvp.chat.ChatPresenter;
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
    private static final int TYPE_RECALLED = 2;   // 已撤回：居中灰色提示行

    /** 长按消息时的操作回调（撤回 / 删除），由 ChatActivity 转交给 Presenter 处理。 */
    public interface MessageActionListener {
        void onRecall(ChatMessage message);
        void onDelete(ChatMessage message);
    }

    private final List<ChatMessage> messages;
    private final String currentUser;
    private final String currentNickname;
    private final String otherNickname;
    private final MessageActionListener actionListener;

    public ChatAdapter(List<ChatMessage> messages, String currentUser, String currentNickname, String otherNickname,
            MessageActionListener actionListener) {
        this.messages = messages;
        this.currentUser = currentUser;
        this.currentNickname = currentNickname != null && !currentNickname.isEmpty() ? currentNickname : currentUser;
        this.otherNickname = otherNickname != null && !otherNickname.isEmpty() ? otherNickname : "?";
        this.actionListener = actionListener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        if (msg.recalled) {
            return TYPE_RECALLED;
        }
        return msg.fromUser.equals(currentUser) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            View v = inf.inflate(R.layout.item_chat_sent, parent, false);
            return new SentHolder(v);
        } else if (viewType == TYPE_RECALLED) {
            View v = inf.inflate(R.layout.item_chat_recalled, parent, false);
            return new RecalledHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof RecalledHolder) {
            RecalledHolder h = (RecalledHolder) holder;
            h.tvHint.setText(msg.fromUser.equals(currentUser) ? "你撤回了一条消息" : "对方撤回了一条消息");
            return;
        }
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

    static class RecalledHolder extends RecyclerView.ViewHolder {
        TextView tvHint;
        RecalledHolder(View v) {
            super(v);
            tvHint = v.findViewById(R.id.tvRecalledHint);
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
            setupLongPress(ivImage, msg);
        } else {
            ivImage.setVisibility(View.GONE);
            ivImage.setOnClickListener(null);
            tvContent.setVisibility(View.VISIBLE);
            tvContent.setText(msg.content);
            setupLongPress(tvContent, msg);
        }
    }

    // 给气泡（文字或图片）挂上长按菜单：撤回（仅本人、2 分钟内可见）+ 删除
    private void setupLongPress(View bubble, ChatMessage msg) {
        bubble.setOnLongClickListener(v -> {
            showActionMenu(v, msg);
            return true;
        });
    }

    // 仿微信的深色气泡菜单：长按消息后，在气泡正上方浮出一个圆角深色条，横向排列「撤回 / 删除」。
    private void showActionMenu(View anchor, ChatMessage msg) {
        boolean canRecall = msg.fromUser.equals(currentUser)
                && !msg.recalled
                && System.currentTimeMillis() - msg.timestamp <= ChatPresenter.RECALL_WINDOW_MS;

        LinearLayout bar = new LinearLayout(anchor.getContext());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(0xF22B2B2B);                 // 半透明深灰
        barBg.setCornerRadius(dp(anchor, 10));
        bar.setBackground(barBg);

        PopupWindow popup = new PopupWindow(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(dp(anchor, 6));

        if (canRecall) {
            bar.addView(buildMenuItem(anchor, "撤回", () -> {
                popup.dismiss();
                if (actionListener != null) actionListener.onRecall(msg);
            }));
            bar.addView(buildDivider(anchor));
        }
        bar.addView(buildMenuItem(anchor, "删除", () -> {
            popup.dismiss();
            if (actionListener != null) actionListener.onDelete(msg);
        }));

        popup.setContentView(bar);

        // 测量后让菜单水平居中于气泡、并浮在其正上方；上方放不下就翻到下方，并把左右夹在屏幕内
        bar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popW = bar.getMeasuredWidth();
        int popH = bar.getMeasuredHeight();
        int screenW = anchor.getResources().getDisplayMetrics().widthPixels;
        int margin = dp(anchor, 8);
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);

        int x = loc[0] + anchor.getWidth() / 2 - popW / 2;
        x = Math.max(margin, Math.min(x, screenW - popW - margin));

        int y = loc[1] - popH - dp(anchor, 6);
        if (y < margin) {
            y = loc[1] + anchor.getHeight() + dp(anchor, 6);   // 上方空间不够，改放气泡下方
        }
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
    }

    // 菜单里的单个操作项：白色文字、点按高亮
    private TextView buildMenuItem(View ref, String text, Runnable onClick) {
        TextView tv = new TextView(ref.getContext());
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(ref, 18), dp(ref, 11), dp(ref, 18), dp(ref, 11));
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setOnClickListener(v -> onClick.run());
        return tv;
    }

    // 菜单项之间的细分隔线
    private View buildDivider(View ref) {
        View divider = new View(ref.getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.max(1, dp(ref, 1)), dp(ref, 22));
        lp.topMargin = dp(ref, 7);
        lp.bottomMargin = dp(ref, 7);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(0x33FFFFFF);
        return divider;
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

        // 保存按钮放到底部居中，避免被状态栏/刘海遮挡，且文字清晰可识别
        TextView download = new TextView(source.getContext());
        download.setText("保存到相册");
        download.setTextColor(Color.WHITE);
        download.setTextSize(14);
        download.setGravity(Gravity.CENTER);
        download.setPadding(dp(source, 24), dp(source, 11), dp(source, 24), dp(source, 11));
        GradientDrawable downloadBg = new GradientDrawable();
        downloadBg.setColor(0x99000000);
        downloadBg.setCornerRadius(dp(source, 24));
        downloadBg.setStroke(dp(source, 1), 0x66FFFFFF);
        download.setBackground(downloadBg);
        FrameLayout.LayoutParams downloadLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        downloadLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        downloadLp.bottomMargin = dp(source, 56);
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
