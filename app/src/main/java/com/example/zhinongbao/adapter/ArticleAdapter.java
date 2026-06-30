package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.Article;
import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(Article article);
    }

    public interface ArticleInteractionDelegate {
        int getArticleLikeCount(int articleId);
        int getCommentCount(int articleId);
        boolean isArticleLiked(int articleId);
        void toggleArticleLike(int articleId);
    }

    private final List<Article> data;
    private final OnItemClickListener listener;
    private final String currentUser;
    private final ArticleInteractionDelegate interactionDelegate;
    private boolean hideCategory = false;

    public ArticleAdapter(List<Article> data, OnItemClickListener listener) {
        this(data, listener, (ArticleInteractionDelegate) null, null);
    }

    public ArticleAdapter(List<Article> data, OnItemClickListener listener,
            ArticleInteractionDelegate interactionDelegate, String currentUser) {
        this.data = data;
        this.listener = listener;
        this.interactionDelegate = interactionDelegate;
        this.currentUser = currentUser;
    }

    public void setHideCategory(boolean hideCategory) {
        this.hideCategory = hideCategory;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Article a = data.get(position);

        holder.tvArticleTitle.setText(a.title);
        holder.tvTime.setText(a.time);
        if (interactionDelegate != null) {
            int likeCount = interactionDelegate.getArticleLikeCount(a.id);
            int commentCount = interactionDelegate.getCommentCount(a.id);
            boolean liked = currentUser != null && interactionDelegate.isArticleLiked(a.id);
            holder.ivLike.setImageResource(liked ? R.mipmap.dianzan : R.mipmap.weidianzan);
            holder.tvLikeCount.setText(String.valueOf(likeCount));
            holder.tvCommentCount.setText(String.valueOf(commentCount));
            View.OnClickListener likeClick = v -> {
                if (currentUser == null || currentUser.isEmpty())
                    return;
                interactionDelegate.toggleArticleLike(a.id);
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION)
                    notifyItemChanged(pos);
            };
            holder.ivLike.setOnClickListener(likeClick);
            holder.tvLikeCount.setOnClickListener(likeClick);
        } else {
            holder.ivLike.setImageResource(R.mipmap.weidianzan);
            holder.tvLikeCount.setText(String.valueOf(a.readCount));
            holder.tvCommentCount.setText("0");
            holder.ivLike.setOnClickListener(null);
            holder.tvLikeCount.setOnClickListener(null);
        }
        if (holder.tvArticleCategory != null) {
            if (hideCategory) {
                holder.tvArticleCategory.setVisibility(View.GONE);
            } else {
                holder.tvArticleCategory.setVisibility(View.VISIBLE);
                String cat = (a.category != null && !a.category.isEmpty()) ? a.category : "热点新闻";
                holder.tvArticleCategory.setText(cat);
            }
        }

        if (a.isDeleted) {
            holder.flDeletedOverlay.setVisibility(View.VISIBLE);
        } else {
            holder.flDeletedOverlay.setVisibility(View.GONE);
        }

        // --- Cover Image ---
        // 封面 = 专门封面（有则用）否则第一张内容配图，统一由 Article.getCoverImage() 解析
        String coverImage = a.getCoverImage();
        if (coverImage != null) {
            try {
                holder.ivArticleThumb.setImageURI(android.net.Uri.parse(coverImage));
            } catch (Exception e) {
                holder.ivArticleThumb.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            // Seed data cover logic
            int fallbackResId = R.drawable.ic_launcher_background;
            switch (a.id) {
                case 5:
                    fallbackResId = R.mipmap.text1;
                    break;
                case 4:
                    fallbackResId = R.mipmap.text2;
                    break;
                case 3:
                    fallbackResId = R.mipmap.text3;
                    break;
                case 2:
                    fallbackResId = R.mipmap.text4;
                    break;
                case 1:
                    fallbackResId = R.mipmap.text5;
                    break;
            }
            holder.ivArticleThumb.setImageResource(fallbackResId);
        }

        if (!a.isDeleted) {
            holder.itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onItemClick(a);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivArticleThumb, ivLike, ivComment;
        TextView tvTime, tvLikeCount, tvCommentCount, tvArticleTitle, tvArticleCategory;
        View flDeletedOverlay;

        VH(View v) {
            super(v);
            tvArticleTitle = v.findViewById(R.id.tvArticleTitle);
            tvTime = v.findViewById(R.id.tvArticleTime);
            tvLikeCount = v.findViewById(R.id.tvArticleLikeCount);
            tvCommentCount = v.findViewById(R.id.tvArticleCommentCount);
            ivLike = v.findViewById(R.id.ivArticleLike);
            ivComment = v.findViewById(R.id.ivArticleComment);
            ivArticleThumb = v.findViewById(R.id.ivArticleThumb);
            flDeletedOverlay = v.findViewById(R.id.flDeletedOverlay);
            tvArticleCategory = v.findViewById(R.id.tvArticleCategory);
        }
    }
}
