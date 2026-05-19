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
