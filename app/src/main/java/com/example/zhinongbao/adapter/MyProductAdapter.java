package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Product;
import java.util.List;

public class MyProductAdapter extends RecyclerView.Adapter<MyProductAdapter.ViewHolder> {

    private final List<Product> list;
    private final OnProductActionListener listener;

    public interface OnProductActionListener {
        void onEdit(Product product);

        void onDelete(Product product, int position);
    }

    public MyProductAdapter(List<Product> list, OnProductActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = list.get(position);
        holder.tvName.setText(p.name);
        holder.tvDesc.setText(p.desc);
        holder.tvPrice.setText(String.format("¥%.2f", p.price));

        String[] cats = p.category != null ? p.category.split(",") : new String[] { "推荐" };
        holder.tvCategory.setText(cats.length > 0 ? cats[0].trim() : "推荐");

        int defaultRes = getDefaultProductImageRes(p.id);
        if (defaultRes != 0) {
            holder.ivCover.setImageResource(defaultRes);
        } else if (p.coverUri != null && !p.coverUri.isEmpty()) {
            String firstImage = p.coverUri.split(",")[0];
            holder.ivCover.setImageURI(android.net.Uri.parse(firstImage));
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_product_placeholder);
        }

        DataManager dm = DataManager.getInstance(holder.itemView.getContext());
        int orderCount = dm.getProductOrderCount(p.id);
        double revenue = dm.getProductSalesRevenue(p.id);
        holder.tvStats.setText(String.format("累计订单: %d  累计销售额: ¥%.2f", orderCount, revenue));

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null)
                listener.onEdit(p);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null)
                listener.onDelete(p, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private int getDefaultProductImageRes(int productId) {
        switch (productId) {
            case 1:
                return R.mipmap.dami1;
            case 2:
                return R.mipmap.muer;
            case 3:
                return R.mipmap.fengmi1;
            case 4:
                return R.mipmap.shucai1;
            case 5:
                return R.mipmap.dongchongxiacao1;
            case 6:
                return R.mipmap.hongshu1;
            case 7:
                return R.mipmap.shanyao1;
            case 8:
                return R.mipmap.yangdujun1;
            case 9:
                return R.mipmap.luronggu1;
            case 10:
                return R.mipmap.tuedan1;
            default:
                return 0;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvName, tvDesc, tvPrice, tvCategory, tvStats, btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivProductCover);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvDesc = itemView.findViewById(R.id.tvProductDesc);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStats = itemView.findViewById(R.id.tvStats);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
