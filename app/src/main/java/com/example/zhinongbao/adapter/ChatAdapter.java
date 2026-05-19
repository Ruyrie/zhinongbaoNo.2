package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.ChatMessage;
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
            h.tvContent.setText(msg.content);
            h.tvTime.setText(timeStr);
        } else {
            ReceivedHolder h = (ReceivedHolder) holder;
            h.tvAvatar.setText(otherNickname.substring(0, 1).toUpperCase());
            h.tvNickname.setText(otherNickname);
            h.tvContent.setText(msg.content);
            h.tvTime.setText(timeStr);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvNickname, tvContent, tvTime;
        SentHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvSentAvatar);
            tvNickname = v.findViewById(R.id.tvSentNickname);
            tvContent = v.findViewById(R.id.tvSentContent);
            tvTime = v.findViewById(R.id.tvSentTime);
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvNickname, tvContent, tvTime;
        ReceivedHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvReceivedAvatar);
            tvNickname = v.findViewById(R.id.tvReceivedNickname);
            tvContent = v.findViewById(R.id.tvReceivedContent);
            tvTime = v.findViewById(R.id.tvReceivedTime);
        }
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
