package com.example.zhinongbao.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.ProductComment;
import java.util.Arrays;
import java.util.List;

/**
 * ============================================================
 * 【商品评价 / Product Comment】Adapter（RecyclerView 适配器）
 * 整体逻辑：头像支持 Base64 或 Uri；配图用一个内嵌的横向小 RecyclerView 展示；
 *   删除按钮只对「自己的评价」或管理员(admin)显示，点击回调外部删除。
 * 数据来源：构造时传入的 List<ProductComment>。
 * 配合的文件：数据模型 model/ProductComment；行布局 res/layout/item_product_comment.xml；
 *   头像工具 utils/ImageUtils；使用方 ProductCommentsActivity。
 * 提示：在 IDE 里搜索「商品评价」可看本组相关文件。
 * ============================================================
 */
public class ProductCommentAdapter extends RecyclerView.Adapter<ProductCommentAdapter.CommentViewHolder> {

    private List<ProductComment> data;
    private String currentUser;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(ProductComment comment);
    }

    public ProductCommentAdapter(List<ProductComment> data, String currentUser) {
        this.data = data;
        this.currentUser = currentUser;
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        ProductComment c = data.get(position);

        holder.tvUsername.setText(c.nickname != null && !c.nickname.isEmpty() ? c.nickname : c.username);
        holder.tvTime.setText(c.time);
        holder.tvContent.setText(c.content);

        if (c.avatarUri != null && !c.avatarUri.isEmpty()) {
            try {
                if (c.avatarUri.startsWith("data:image")) {
                    com.example.zhinongbao.utils.ImageUtils.setAvatarFromBase64(holder.ivAvatar, c.avatarUri);
                } else {
                    holder.ivAvatar.setImageURI(Uri.parse(c.avatarUri));
                }
            } catch (Exception e) {
                holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        }

        if (c.username.equals(currentUser) || "admin".equals(currentUser)) {
            holder.tvDelete.setVisibility(View.VISIBLE);
            holder.tvDelete.setOnClickListener(v -> {
                if (deleteListener != null)
                    deleteListener.onDelete(c);
            });
        } else {
            holder.tvDelete.setVisibility(View.GONE);
        }

        if (c.images != null && !c.images.isEmpty()) {
            holder.rvCommentImages.setVisibility(View.VISIBLE);
            holder.rvCommentImages.setLayoutManager(
                    new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));

            List<String> uris = Arrays.asList(c.images.split(","));
            holder.rvCommentImages.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    ImageView iv = new ImageView(parent.getContext());
                    int size = (int) (100 * parent.getContext().getResources().getDisplayMetrics().density);
                    ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(size, size);
                    params.setMarginEnd((int) (8 * parent.getContext().getResources().getDisplayMetrics().density));
                    iv.setLayoutParams(params);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setBackgroundColor(0xFFEEEEEE);
                    return new RecyclerView.ViewHolder(iv) {
                    };
                }

                @Override
                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderImg, int position) {
                    ImageView iv = (ImageView) holderImg.itemView;
                    try {
                        iv.setImageURI(Uri.parse(uris.get(position)));
                    } catch (Exception e) {
                        iv.setImageResource(R.drawable.ic_launcher_background);
                    }
                }

                @Override
                public int getItemCount() {
                    return uris.size();
                }
            });
        } else {
            holder.rvCommentImages.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername;
        TextView tvTime;
        TextView tvDelete;
        TextView tvContent;
        RecyclerView rvCommentImages;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDelete = itemView.findViewById(R.id.tvDelete);
            tvContent = itemView.findViewById(R.id.tvContent);
            rvCommentImages = itemView.findViewById(R.id.rvCommentImages);
        }
    }
}
