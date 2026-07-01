package com.example.zhinongbao.adapter;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import java.util.List;

/**
 * ============================================================
 * 【商品图片轮播 / Product Image】Adapter（RecyclerView 适配器）
 * 整体逻辑：不吃固定布局文件，代码里直接 new 一个铺满的 ImageView 作为每一项；
 *   每项数据可以是 Integer(资源 id) 或 String(图片 Uri)，绑定时按类型分别显示。
 * 数据来源：构造时传入的 List<Object>（由商品详情页组装：种子商品用资源 id，
 *   用户商品用 Uri）。
 * 配合的文件：使用方 ProductDetailActivity（配合 ViewPager2/RecyclerView 做轮播）。
 * 提示：在 IDE 里搜索「商品详情」可看本组相关文件。
 * ============================================================
 */
public class ProductImageAdapter extends RecyclerView.Adapter<ProductImageAdapter.ImageViewHolder> {

    private List<Object> imageList; // 每项可以是 Integer(资源 ID) 或 String(图片 Uri)
    private final ImageView.ScaleType scaleType;

    public ProductImageAdapter(List<Object> imageList) {
        this(imageList, ImageView.ScaleType.CENTER_CROP);
    }

    public ProductImageAdapter(List<Object> imageList, ImageView.ScaleType scaleType) {
        this.imageList = imageList;
        this.scaleType = scaleType;
    }

    public void setImageList(List<Object> imageList) {
        this.imageList = imageList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(scaleType);
        return new ImageViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Object item = imageList.get(position);
        if (item instanceof Integer) {
            holder.imageView.setImageResource((Integer) item);
        } else if (item instanceof String) {
            try {
                holder.imageView.setImageURI(Uri.parse((String) item));
            } catch (Exception e) {
                holder.imageView.setImageResource(R.drawable.ic_product_placeholder);
            }
        }
    }

    @Override
    public int getItemCount() {
        return imageList == null ? 0 : imageList.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}
