package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.User;
import java.util.List;

/**
 * ============================================================
 * 【账号列表适配器 / UserAdapter】Adapter（RecyclerView 适配器）
 * 整体逻辑（关键步骤）：
 *   1. onCreateViewHolder 用 item_user 布局创建行视图。
 *   2. onBindViewHolder 填充用户名，按 avatarUri 显示头像或首字母占位。
 *   3. 行短按回调 onShortClick、长按回调 onLongClick，由外部（Activity）处理。
 * 数据来源：数据列表由外部传入（AccountManagerActivity 经 Presenter 从
 *   UserRepository 取得），本类不直接碰数据库；头像解码用 utils/ImageUtils。
 * 配合的文件：使用方 = AccountManagerActivity；行布局 = res/layout/item_user.xml；
 *   模型 = model/User；工具 = utils/ImageUtils。
 * 在 MVP 数据流中的位置：View 的一部分（列表渲染），不参与业务逻辑。
 * 提示：在 IDE 里搜索「账号列表适配器」可看本组相关文件。
 * ============================================================
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

    public interface OnItemListener {
        void onShortClick(User user);

        void onLongClick(User user);
    }

    private final List<User> data;
    private final OnItemListener listener;

    public UserAdapter(List<User> data, OnItemListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        User u = data.get(position);
        holder.tvUsername.setText(u.username);
        // 首字母头像
        String initial = u.username.length() > 0
                ? String.valueOf(u.username.charAt(0)).toUpperCase()
                : "?";
        holder.tvInitial.setText(initial);

        if (u.avatarUri != null) {
            try {
                if (u.avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(holder.ivAvatar, u.avatarUri);
                } else {
                    holder.ivAvatar.setImageURI(android.net.Uri.parse(u.avatarUri));
                }
                holder.ivAvatar.setVisibility(View.VISIBLE);
                holder.tvInitial.setVisibility(View.GONE);
            } catch (Exception e) {
                holder.ivAvatar.setVisibility(View.GONE);
                holder.tvInitial.setVisibility(View.VISIBLE);
            }
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvInitial.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> listener.onShortClick(u));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(u);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvUsername, tvInitial;
        ImageView ivAvatar;

        VH(View v) {
            super(v);
            tvUsername = v.findViewById(R.id.tvUsername);
            tvInitial = v.findViewById(R.id.tvUserInitial);
            ivAvatar = v.findViewById(R.id.ivUserAvatar);
        }
    }
}
