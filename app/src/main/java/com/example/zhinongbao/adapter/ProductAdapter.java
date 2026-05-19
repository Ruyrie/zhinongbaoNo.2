package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.Product;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public interface OnAddCartListener {
        void onAddCart(Product product);
    }

    private final List<Product> data;
    private final OnItemClickListener clickListener;
    private OnAddCartListener addCartListener;
    private boolean compactMode = false;

    public ProductAdapter(List<Product> data, OnItemClickListener listener) {
        this.data = data;
        this.clickListener = listener;
    }

    public void setOnAddCartListener(OnAddCartListener l) {
        this.addCartListener = l;
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Product p = data.get(position);

        // 动态设置图片高度，产生瀑布流错落效果
        int[] heights = compactMode
                ? new int[] { 130, 150, 120, 145, 135, 155 }
                : new int[] { 200, 260, 180, 240, 220, 280 };
        int heightDp = heights[p.id % heights.length];
        int heightPx = (int) (heightDp * holder.itemView.getContext().getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams lp = holder.ivProduct.getLayoutParams();
        lp.height = heightPx;
        holder.ivProduct.setLayoutParams(lp);

        switch (p.id) {
            case 1:
                holder.ivProduct.setImageResource(R.mipmap.dami1);
                break; // 东北大米
            case 2:
                holder.ivProduct.setImageResource(R.mipmap.muer);
                break; // 有机黑木耳
            case 3:
                holder.ivProduct.setImageResource(R.mipmap.fengmi1);
                break; // 农家蜂蜜
            case 4:
                holder.ivProduct.setImageResource(R.mipmap.shucai1);
                break; // 绿色蔬菜礼盒
            case 5:
                holder.ivProduct.setImageResource(R.mipmap.dongchongxiacao1);
                break; // 冬虫夏草
            case 6:
                holder.ivProduct.setImageResource(R.mipmap.hongshu1);
                break; // 红薯
            case 7:
                holder.ivProduct.setImageResource(R.mipmap.shanyao1);
                break; // 山药
            case 8:
                holder.ivProduct.setImageResource(R.mipmap.yangdujun1);
                break; // 羊肚菌
            case 9:
                holder.ivProduct.setImageResource(R.mipmap.luronggu1);
                break; // 鹿茸菇
            case 10:
                holder.ivProduct.setImageResource(R.mipmap.tuedan1);
                break; // 土鹅蛋
            default:
                if (p.coverUri != null && !p.coverUri.isEmpty()) {
                    String firstUri = p.coverUri.split(",")[0];
                    try {
                        holder.ivProduct.setImageURI(android.net.Uri.parse(firstUri));
                    } catch (Exception e) {
                        holder.ivProduct.setImageResource(R.drawable.ic_product_placeholder);
                    }
                } else {
                    holder.ivProduct.setImageResource(R.drawable.ic_product_placeholder);
                }
        }
        holder.tvName.setText(p.name);
        // Show price without ¥ prefix (¥ is a separate TextView in XML)
        holder.tvPrice.setText(String.valueOf((long) p.price % 1 == 0
                ? String.format("%d", (long) p.price)
                : String.format("%.2f", p.price)));
        holder.tvViews.setText(String.valueOf(Math.max(0, p.viewCount)));
        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(p));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice, tvViews;

        VH(View v) {
            super(v);
            ivProduct = v.findViewById(R.id.ivProductImage);
            tvName = v.findViewById(R.id.tvProductName);
            tvPrice = v.findViewById(R.id.tvProductPrice);
            tvViews = v.findViewById(R.id.tvProductViews);
        }
    }
}
