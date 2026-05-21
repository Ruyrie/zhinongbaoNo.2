package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.Comment;
import java.util.List;

/** 评论列表适配器（抖音 / 小红书风格） */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {

    public interface OnDeleteListener {
        void onDelete(Comment comment);
    }

    public interface CommentInteractionDelegate {
        void likeComment(int commentId);
        void unlikeComment(int commentId);
    }

    private final List<Comment> data;
    private final String currentUser;
    private final String articleAuthor;
    private final CommentInteractionDelegate interactionDelegate;
    private OnDeleteListener deleteListener;

    public CommentAdapter(List<Comment> data, String currentUser,
            String articleAuthor, CommentInteractionDelegate interactionDelegate) {
        this.data = data;
        this.currentUser = currentUser;
        this.articleAuthor = articleAuthor;
        this.interactionDelegate = interactionDelegate;
    }

    public void setOnDeleteListener(OnDeleteListener l) {
        this.deleteListener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Comment c = data.get(position);

        // Avatar initial
        String initial = "U";
        if (c.nickname != null && !c.nickname.isEmpty()) {
            initial = String.valueOf(c.nickname.charAt(0)).toUpperCase();
        } else if (c.username != null && !c.username.isEmpty()) {
            initial = String.valueOf(c.username.charAt(0)).toUpperCase();
        }
        h.tvAvatar.setText(initial);

        if (c.avatarUri != null) {
            try {
                if (c.avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(h.ivAvatarImg, c.avatarUri);
                } else {
                    h.ivAvatarImg.setImageURI(android.net.Uri.parse(c.avatarUri));
                }
                h.ivAvatarImg.setVisibility(View.VISIBLE);
                h.tvAvatar.setVisibility(View.GONE);
            } catch (Exception e) {
                h.ivAvatarImg.setVisibility(View.GONE);
                h.tvAvatar.setVisibility(View.VISIBLE);
            }
        } else {
            h.ivAvatarImg.setVisibility(View.GONE);
            h.tvAvatar.setVisibility(View.VISIBLE);
        }

        h.tvUsername.setText(c.nickname != null && !c.nickname.isEmpty() ? c.nickname : c.username);
        h.tvContent.setText(c.content);
        h.tvTime.setText(c.time);
        h.tvLikeCount.setText(c.likeCount > 0 ? String.valueOf(c.likeCount) : "");

        // Delete button: visible for own comment OR article author
        boolean canDelete = c.username.equals(currentUser) || articleAuthor.equals(currentUser);
        h.tvDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);

        // Like state
        applyLikeState(h, c.isLikedByMe);

        // Like click
        h.layoutLike.setOnClickListener(v -> {
            if (interactionDelegate == null || currentUser == null || currentUser.isEmpty()) {
                return;
            }
            if (c.isLikedByMe) {
                interactionDelegate.unlikeComment(c.id);
                c.likeCount = Math.max(0, c.likeCount - 1);
                c.isLikedByMe = false;
            } else {
                interactionDelegate.likeComment(c.id);
                c.likeCount++;
                c.isLikedByMe = true;
            }
            applyLikeState(h, c.isLikedByMe);
            h.tvLikeCount.setText(c.likeCount > 0 ? String.valueOf(c.likeCount) : "");
        });

        // Delete click
        h.tvDelete.setOnClickListener(v -> {
            if (deleteListener != null)
                deleteListener.onDelete(c);
        });
    }

    private void applyLikeState(VH h, boolean liked) {
        if (liked) {
            h.ivLikeBtn.setImageResource(R.mipmap.dianzan);
        } else {
            h.ivLikeBtn.setImageResource(R.mipmap.weidianzan);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvUsername, tvContent, tvTime, tvDelete;
        ImageView ivLikeBtn;
        TextView tvLikeCount;
        LinearLayout layoutLike;
        android.widget.ImageView ivAvatarImg;

        VH(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvCommentAvatar);
            ivAvatarImg = v.findViewById(R.id.ivCommentAvatar);
            tvUsername = v.findViewById(R.id.tvCommentUsername);
            tvContent = v.findViewById(R.id.tvCommentContent);
            tvTime = v.findViewById(R.id.tvCommentTime);
            tvDelete = v.findViewById(R.id.tvDeleteComment);
            ivLikeBtn = v.findViewById(R.id.tvCommentLikeBtn);
            tvLikeCount = v.findViewById(R.id.tvCommentLikeCount);
            layoutLike = v.findViewById(R.id.layoutCommentLike);
        }
    }
}
