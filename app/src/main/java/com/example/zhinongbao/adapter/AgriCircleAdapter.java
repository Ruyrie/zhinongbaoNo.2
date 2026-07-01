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
import com.example.zhinongbao.model.Article;
import com.example.zhinongbao.model.User;
import java.util.List;

/**
 * ============================================================
 * 【农友圈动态 / Agri Circle Post】Adapter（RecyclerView 适配器）
 * 整体逻辑：onCreateViewHolder 用 item_agri_circle_post.xml 生成卡片；onBindViewHolder
 *   把第 position 条动态填进控件并绑定点击。多图用 ViewPager2 + ProductImageAdapter 轮播，
 *   右上角显示「当前/总数」指示器。已被作者删除的动态显示占位提示，仅保留取消点赞。
 *   点赞/关注按钮只做即时视觉更新，真正写库通过 OnActionListener 回调交给外部处理。
 * 数据来源：构造时传入的 List<Article>（由 Fragment/Activity 从 Presenter 拿到），
 *   本类不查数据库；点赞数/评论数/是否已赞/是否已关注/用户角色等通过
 *   CircleInteractionDelegate 现查（最终仍走 Presenter → Repository）。
 * 权限/显示规则：只有卖家作者才显示「进店铺」；只有非自己且未关注才显示「关注」。
 * 配合的文件：数据模型 model/Article、model/User；卡片布局 res/layout/item_agri_circle_post.xml；
 *   图片轮播 adapter/ProductImageAdapter；头像工具 utils/ImageUtils；
 *   使用方 AgriCircleFragment、MyCirclePostsActivity（各自实现两个回调接口）。
 * 在 MVP 数据流中的位置：View 层的一部分（把 Presenter 提供的数据展示出来）。
 * 提示：在 IDE 里搜索「农友圈动态」可看本组相关文件。
 * ============================================================
 */
public class AgriCircleAdapter extends RecyclerView.Adapter<AgriCircleAdapter.ViewHolder> {

    public interface OnActionListener {
        void onItemClick(Article article);
        void onLikeClick(Article article, int position);
        void onCommentClick(Article article);
        void onEnterStore(Article article);
        void onFollow(Article article, int position);
    }

    public interface CircleInteractionDelegate {
        int getCircleLikeCount(int articleId);
        int getCommentCount(int articleId);
        boolean isCircleLiked(int articleId);
        boolean isFollowing(String author);
        int getUserRole(String username);
    }

    private final List<Article> items;
    private final String currentUser;
    private final CircleInteractionDelegate interactionDelegate;
    private final OnActionListener listener;

    public AgriCircleAdapter(List<Article> items, String currentUser,
            CircleInteractionDelegate interactionDelegate, OnActionListener listener) {
        this.items = items;
        this.currentUser = currentUser;
        this.interactionDelegate = interactionDelegate;
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

        // 已被作者删除的动态：显示占位提示，仅保留取消点赞
        if (a.isDeleted) {
            showInitial(h, "?");
            h.tvNickname.setText("该动态已被删除");
            h.tvTime.setText("");
            h.tvContent.setText("抱歉，该动态已被作者删除。");
            h.layoutImages.setVisibility(View.GONE);
            h.flDeletedOverlay.setVisibility(View.VISIBLE); // 与文章一致的「已删除」遮罩
            h.btnEnterStore.setVisibility(View.GONE);
            h.btnFollow.setVisibility(View.GONE);
            h.tvReadCount.setText("");
            boolean liked = interactionDelegate.isCircleLiked(a.id);
            h.tvLikeCount.setText(String.valueOf(interactionDelegate.getCircleLikeCount(a.id)));
            h.ivLikeIcon.setImageResource(liked ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
            h.tvCommentCount.setText(String.valueOf(interactionDelegate.getCommentCount(a.id)));
            h.itemView.setOnClickListener(null);
            h.btnComment.setOnClickListener(null);
            h.btnLike.setOnClickListener(v -> listener.onLikeClick(a, h.getAdapterPosition()));
            return;
        }

        // 正常动态：隐藏「已删除」遮罩（复用 ViewHolder 时必须显式还原）
        h.flDeletedOverlay.setVisibility(View.GONE);

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

        // 图片轮播
        if (a.coverUri != null && !a.coverUri.isEmpty()) {
            h.layoutImages.setVisibility(View.VISIBLE);
            java.util.List<Object> images = new java.util.ArrayList<>();
            for (String uri : a.coverUri.split(",")) {
                if (!uri.trim().isEmpty())
                    images.add(uri.trim());
            }
            h.imageCount = images.size();
            // CENTER_CROP：图片铺满整个封面轮播，超出部分裁剪，无空白边
            h.vpImages.setAdapter(new com.example.zhinongbao.adapter.ProductImageAdapter(
                    images, ImageView.ScaleType.CENTER_CROP));
            h.vpImages.setCurrentItem(0, false);
            h.tvImageIndicator.setVisibility(images.size() > 1 ? View.VISIBLE : View.GONE);
            h.tvImageIndicator.setText("1/" + images.size());
        } else {
            h.layoutImages.setVisibility(View.GONE);
        }

        // 点赞
        int likeCount = interactionDelegate.getCircleLikeCount(a.id);
        boolean liked = interactionDelegate.isCircleLiked(a.id);
        h.tvLikeCount.setText(String.valueOf(likeCount));
        h.ivLikeIcon.setImageResource(liked ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);

        // 评论数
        h.tvCommentCount.setText(String.valueOf(interactionDelegate.getCommentCount(a.id)));
        h.tvReadCount.setText(a.readCount + " 浏览");

        // 进店铺（卖家身份的作者）
        int authorRole = interactionDelegate.getUserRole(a.author);
        boolean isSeller = authorRole == User.ROLE_SELLER || authorRole == User.ROLE_BOTH;
        h.btnEnterStore.setVisibility(isSeller ? View.VISIBLE : View.GONE);
        if (isSeller) {
            h.btnEnterStore.setOnClickListener(v -> listener.onEnterStore(a));
        }

        // +关注（非自己且未关注时显示）
        boolean isOwn = a.author.equals(currentUser);
        boolean following = interactionDelegate.isFollowing(a.author);
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
        ImageView ivAvatar, ivLikeIcon;
        View layoutImages;
        View flDeletedOverlay;
        androidx.viewpager2.widget.ViewPager2 vpImages;
        TextView tvAvatarInitial, tvNickname, tvTime, tvContent;
        TextView tvLikeCount, tvCommentCount, tvReadCount, tvImageIndicator;
        View btnLike, btnComment;
        TextView btnEnterStore, btnFollow;
        int imageCount;

        ViewHolder(View v) {
            super(v);
            ivAvatar       = v.findViewById(R.id.ivPostAvatar);
            ivLikeIcon     = v.findViewById(R.id.ivLikeIcon);
            layoutImages   = v.findViewById(R.id.layoutPostImages);
            flDeletedOverlay = v.findViewById(R.id.flDeletedOverlay);
            vpImages       = v.findViewById(R.id.vpPostImages);
            tvImageIndicator = v.findViewById(R.id.tvPostImageIndicator);
            tvAvatarInitial= v.findViewById(R.id.tvPostAvatarInitial);
            tvNickname     = v.findViewById(R.id.tvPostNickname);
            tvTime         = v.findViewById(R.id.tvPostTime);
            tvContent      = v.findViewById(R.id.tvPostContent);
            tvLikeCount    = v.findViewById(R.id.tvLikeCount);
            tvCommentCount = v.findViewById(R.id.tvCommentCount);
            tvReadCount    = v.findViewById(R.id.tvPostReadCount);
            btnLike        = v.findViewById(R.id.btnPostLike);
            btnComment     = v.findViewById(R.id.btnPostComment);
            btnEnterStore  = v.findViewById(R.id.btnEnterStore);
            btnFollow      = v.findViewById(R.id.btnFollow);

            // 仅注册一次翻页回调，避免复用 ViewHolder 时重复注册导致指示器错乱
            vpImages.registerOnPageChangeCallback(
                    new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageSelected(int page) {
                            if (imageCount > 0) {
                                tvImageIndicator.setText((page + 1) + "/" + imageCount);
                            }
                        }
                    });
        }
    }
}
