package com.example.zhinongbao.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.User;
import java.util.List;

public class AgriCircleAdapter extends RecyclerView.Adapter<AgriCircleAdapter.ViewHolder> {

    public interface OnActionListener {
        void onItemClick(Article article);
        void onLikeClick(Article article, int position);
        void onCommentClick(Article article);
        void onEnterStore(Article article);
        void onFollow(Article article, int position);
    }

    private final List<Article> items;
    private final String currentUser;
    private final DataManager dm;
    private final OnActionListener listener;

    public AgriCircleAdapter(List<Article> items, String currentUser,
            DataManager dm, OnActionListener listener) {
        this.items = items;
        this.currentUser = currentUser;
        this.dm = dm;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_agri_circle_post, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Article a = items.get(position);

        // 昵称 & 时间
        String nick = (a.authorNickname != null && !a.authorNickname.isEmpty())
                ? a.authorNickname : a.author;
        h.tvNickname.setText(nick);
        h.tvTime.setText(a.time);

        // 头像
        String avatarUri = a.authorAvatarUri;
        if (avatarUri != null && !avatarUri.isEmpty()) {
            try {
                if (avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils
                            .setAvatarFromBase64(h.ivAvatar, avatarUri);
                } else {
                    h.ivAvatar.setImageURI(Uri.parse(avatarUri));
                }
                h.ivAvatar.setVisibility(View.VISIBLE);
                h.tvAvatarInitial.setVisibility(View.GONE);
            } catch (Exception e) {
                showInitial(h, nick);
            }
        } else {
            showInitial(h, nick);
        }

        // 正文
        h.tvContent.setText(a.content);

        // 封面图
        if (a.coverUri != null && !a.coverUri.isEmpty()) {
            h.ivCover.setVisibility(View.VISIBLE);
            try {
                if (a.coverUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils
                            .setAvatarFromBase64(h.ivCover, a.coverUri);
                } else {
                    h.ivCover.setImageURI(Uri.parse(a.coverUri));
                }
            } catch (Exception e) {
                h.ivCover.setVisibility(View.GONE);
            }
        } else {
            h.ivCover.setVisibility(View.GONE);
        }

        // 点赞
        int likeCount = dm.getArticleLikeCount(a.id);
        boolean liked = dm.isArticleLiked(currentUser, a.id);
        h.tvLikeCount.setText(String.valueOf(likeCount));
        h.ivLikeIcon.setImageResource(liked ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);

        // 评论数
        h.tvCommentCount.setText(String.valueOf(dm.getCommentCount(a.id)));

        // 进店铺（卖家身份的作者）
        int authorRole = dm.getUserRole(a.author);
        boolean isSeller = authorRole == User.ROLE_SELLER || authorRole == User.ROLE_BOTH;
        h.btnEnterStore.setVisibility(isSeller ? View.VISIBLE : View.GONE);
        if (isSeller) {
            h.btnEnterStore.setOnClickListener(v -> listener.onEnterStore(a));
        }

        // +关注（非自己且未关注时显示）
        boolean isOwn = a.author.equals(currentUser);
        boolean following = dm.isFollowing(currentUser, a.author);
        h.btnFollow.setVisibility((!isOwn && !following) ? View.VISIBLE : View.GONE);
        h.btnFollow.setOnClickListener(v -> listener.onFollow(a, h.getAdapterPosition()));

        // 点击事件
        h.itemView.setOnClickListener(v -> listener.onItemClick(a));
        h.btnLike.setOnClickListener(v -> listener.onLikeClick(a, h.getAdapterPosition()));
        h.btnComment.setOnClickListener(v -> listener.onCommentClick(a));
    }

    private void showInitial(ViewHolder h, String nick) {
        h.ivAvatar.setVisibility(View.GONE);
        h.tvAvatarInitial.setVisibility(View.VISIBLE);
        h.tvAvatarInitial.setText(nick.isEmpty() ? "?" :
                String.valueOf(nick.charAt(0)).toUpperCase());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivCover, ivLikeIcon;
        TextView tvAvatarInitial, tvNickname, tvTime, tvContent;
        TextView tvLikeCount, tvCommentCount;
        View btnLike, btnComment;
        TextView btnEnterStore, btnFollow;

        ViewHolder(View v) {
            super(v);
            ivAvatar       = v.findViewById(R.id.ivPostAvatar);
            ivCover        = v.findViewById(R.id.ivPostCover);
            ivLikeIcon     = v.findViewById(R.id.ivLikeIcon);
            tvAvatarInitial= v.findViewById(R.id.tvPostAvatarInitial);
            tvNickname     = v.findViewById(R.id.tvPostNickname);
            tvTime         = v.findViewById(R.id.tvPostTime);
            tvContent      = v.findViewById(R.id.tvPostContent);
            tvLikeCount    = v.findViewById(R.id.tvLikeCount);
            tvCommentCount = v.findViewById(R.id.tvCommentCount);
            btnLike        = v.findViewById(R.id.btnPostLike);
            btnComment     = v.findViewById(R.id.btnPostComment);
            btnEnterStore  = v.findViewById(R.id.btnEnterStore);
            btnFollow      = v.findViewById(R.id.btnFollow);
        }
    }
}
