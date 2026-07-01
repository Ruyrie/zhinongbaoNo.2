package com.example.zhinongbao.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import java.util.List;

/**
 * ============================================================
 * 【图片选择适配器 / ImagePickerAdapter】Adapter（RecyclerView 适配器）
 * 整体逻辑（关键步骤）：
 *   1. getItemCount 返回「已选数 + 1」但不超过 maxImages（末位是添加按钮）。
 *   2. onBindViewHolder：最后一格显示添加占位并回调 onAddClick；其余格显示图片
 *      并提供删除按钮回调 onDeleteClick。
 * 数据来源：图片 Uri 列表由外部传入并维护，本类不涉及 Repository 与数据库。
 * 配合的文件：行布局 = res/layout/item_product_image_picker.xml；
 *   通常由发布商品/文章等需要多图上传的页面使用。
 * 在 MVP 数据流中的位置：View 的一部分（列表渲染），不参与业务逻辑。
 * 提示：在 IDE 里搜索「图片选择适配器」可看本组相关文件。
 * ============================================================
 */
public class ImagePickerAdapter extends RecyclerView.Adapter<ImagePickerAdapter.PickerViewHolder> {

    private List<Uri> imageUris;
    private int maxImages;
    private OnImagePickerClickListener listener;

    public interface OnImagePickerClickListener {
        void onAddClick();

        void onDeleteClick(int position);
    }

    public ImagePickerAdapter(List<Uri> imageUris, int maxImages, OnImagePickerClickListener listener) {
        this.imageUris = imageUris;
        this.maxImages = maxImages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_image_picker, parent, false);
        return new PickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PickerViewHolder holder, int position) {
        if (position == imageUris.size()) {
            // Add button
            holder.llAddPlaceholder.setVisibility(View.VISIBLE);
            holder.ivPickerImage.setVisibility(View.GONE);
            holder.ivDeleteImage.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onAddClick();
            });
        } else {
            // Image item
            holder.llAddPlaceholder.setVisibility(View.GONE);
            holder.ivPickerImage.setVisibility(View.VISIBLE);
            holder.ivDeleteImage.setVisibility(View.VISIBLE);

            holder.ivPickerImage.setImageURI(imageUris.get(position));

            holder.ivDeleteImage.setOnClickListener(v -> {
                if (listener != null)
                    listener.onDeleteClick(position);
            });
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return Math.min(imageUris.size() + 1, maxImages);
    }

    static class PickerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPickerImage;
        ImageView ivDeleteImage;
        LinearLayout llAddPlaceholder;

        PickerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPickerImage = itemView.findViewById(R.id.ivPickerImage);
            ivDeleteImage = itemView.findViewById(R.id.ivDeleteImage);
            llAddPlaceholder = itemView.findViewById(R.id.llAddPlaceholder);
        }
    }
}
