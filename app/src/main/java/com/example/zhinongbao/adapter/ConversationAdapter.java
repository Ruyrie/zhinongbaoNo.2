package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.ConversationItem;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ============================================================
 * 【消息列表-会话 / Conversation】Adapter（RecyclerView 适配器）
 * 整体逻辑：onCreateViewHolder 用 item_conversation.xml 生成行视图；
 *   onBindViewHolder 把第 position 条会话填进控件，未读数为 0 时隐藏红点、超过 99 显示「99+」；
 *   点击整行回调 OnItemClickListener（外部跳聊天页），长按回调 OnItemLongPressListener
 *   （外部弹删除确认）。formatTime 把时间戳按「今天显示时分/昨天/更早显示月日」友好展示。
 * 数据来源：构造时传入的 List<ConversationItem>（由 MessageFragment 从 Presenter 拿到），
 *   本类不查数据库。
 * 配合的文件：数据模型 model/ConversationItem；行布局 res/layout/item_conversation.xml；
 *   使用方 MessageFragment（实现点击/长按回调）。
 * MVP 数据流位置：本类属于 View 层，只负责把数据画到屏幕上。
 * 提示：在 IDE 里搜索「消息」可看本组相关文件。
 * ============================================================
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(ConversationItem item);
    }

    public interface OnItemLongPressListener {
        void onLongPress(ConversationItem item, int position);
    }

    private final List<ConversationItem> items;
    private final OnItemClickListener listener;
    private OnItemLongPressListener longPressListener;

    public ConversationAdapter(List<ConversationItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setOnItemLongPressListener(OnItemLongPressListener l) {
        this.longPressListener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ConversationItem item = items.get(position);
        String display = item.displayName != null && !item.displayName.isEmpty()
                ? item.displayName : item.otherUser;
        h.tvAvatar.setText(display.substring(0, 1).toUpperCase());
        h.tvName.setText(display);
        h.tvLastMsg.setText(item.lastMessage);
        h.tvTime.setText(formatTime(item.lastTimestamp));

        if (item.unreadCount > 0) {
            h.tvUnread.setVisibility(View.VISIBLE);
            h.tvUnread.setText(item.unreadCount > 99 ? "99+" : String.valueOf(item.unreadCount));
        } else {
            h.tvUnread.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(item));
        h.itemView.setOnLongClickListener(v -> {
            if (longPressListener != null) {
                int pos = h.getAdapterPosition();
                if (pos != RecyclerView.NO_ID) {
                    longPressListener.onLongPress(item, pos);
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvLastMsg, tvTime, tvUnread;
        ViewHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvConvAvatar);
            tvName = v.findViewById(R.id.tvConvName);
            tvLastMsg = v.findViewById(R.id.tvConvLastMsg);
            tvTime = v.findViewById(R.id.tvConvTime);
            tvUnread = v.findViewById(R.id.tvConvUnread);
        }
    }

    private String formatTime(long ts) {
        if (ts == 0) return "";
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

        if (isYesterday) return "昨天";

        return new SimpleDateFormat("MM/dd", Locale.CHINA).format(new Date(ts));
    }
}
