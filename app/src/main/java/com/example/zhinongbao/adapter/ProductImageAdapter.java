package com.example.zhinongbao.adapter;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import java.util.List;

public class ProductImageAdapter extends RecyclerView.Adapter<ProductImageAdapter.ImageViewHolder> {

    private List<Object> imageList; // 可以是 Integer (资源 ID) 或 String (Uri)

    public ProductImageAdapter(List<Object> imageList) {
        this.imageList = imageList;
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
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
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
