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
import com.example.zhinongbao.model.Product;
import java.util.List;
import java.util.Locale;

/**
 * ============================================================
 * 【商品列表(单列) / Product List】Adapter（RecyclerView 适配器）
 * 整体逻辑：优先用 coverUri（支持 res:// 资源或普通 Uri），种子商品回退到固定
 *   mipmap 图，否则占位图；价格格式化去掉多余的 .00。
 * 数据来源：构造时传入的 List<Product>。
 * 配合的文件：模型 model/Product；行布局 res/layout/item_product_list.xml。
 * 提示：在 IDE 里搜索「商品列表」可看本组相关文件。
 * ============================================================
 */
public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {

    private final List<Product> products;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public ProductListAdapter(List<Product> products, OnItemClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = products.get(position);
        holder.tvName.setText(p.name);

        // Format price to match screenshot: main integer big, decimals (if any) or just
        // ".00"
        holder.tvPrice.setText(String.format(Locale.getDefault(), "%.2f", p.price).replace(".00", ""));

        if (p.coverUri != null && !p.coverUri.isEmpty()) {
            if (p.coverUri.startsWith("res://")) {
                int resId = Integer.parseInt(p.coverUri.replace("res://", ""));
                holder.ivImage.setImageResource(resId);
            } else {
                holder.ivImage.setImageURI(Uri.parse(p.coverUri));
            }
        } else {
            switch (p.id) {
                case 1:
                    holder.ivImage.setImageResource(R.mipmap.dami1);
                    break; // 东北大米
                case 2:
                    holder.ivImage.setImageResource(R.mipmap.muer);
                    break; // 有机黑木耳
                case 3:
                    holder.ivImage.setImageResource(R.mipmap.fengmi1);
                    break; // 农家蜂蜜
                case 4:
                    holder.ivImage.setImageResource(R.mipmap.shucai1);
                    break; // 绿色蔬菜礼盒
                case 5:
                    holder.ivImage.setImageResource(R.mipmap.dongchongxiacao1);
                    break; // 冬虫夏草
                case 6:
                    holder.ivImage.setImageResource(R.mipmap.hongshu1);
                    break; // 红薯
                case 7:
                    holder.ivImage.setImageResource(R.mipmap.shanyao1);
                    break; // 山药
                case 8:
                    holder.ivImage.setImageResource(R.mipmap.yangdujun1);
                    break; // 羊肚菌
                case 9:
                    holder.ivImage.setImageResource(R.mipmap.luronggu1);
                    break; // 鹿茸菇
                case 10:
                    holder.ivImage.setImageResource(R.mipmap.tuedan1);
                    break; // 土鹅蛋
                default:
                    holder.ivImage.setImageResource(R.drawable.ic_product_placeholder);
                    break;
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName;
        TextView tvPrice;

        ViewHolder(View view) {
            super(view);
            ivImage = view.findViewById(R.id.ivProductImage);
            tvName = view.findViewById(R.id.tvProductName);
            tvPrice = view.findViewById(R.id.tvProductPrice);
        }
    }
}
