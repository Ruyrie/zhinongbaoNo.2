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

/**
 * ============================================================
 * 【我的货品 / My Product】Adapter（RecyclerView 适配器）
 * 整体逻辑：已下架商品名前加「已下架」标记并降低透明度；封面优先用内置示例图，
 *   否则用 coverUri 第一张图，再否则用占位图；销量统计通过 ProductStatsDelegate 回调宿主取得。
 * 数据来源：构造时传入的 List<Product>；统计数据由外部委托（Delegate）向 Presenter 取。
 * 配合的文件：模型 model/Product；行布局 res/layout/item_my_product.xml；
 *   宿主 activity/SellerMyProductsActivity（实现两个回调接口）。
 * 提示：在 IDE 里搜索「我的货品」可看本组相关文件。
 * ============================================================
 */
public class MyProductAdapter extends RecyclerView.Adapter<MyProductAdapter.ViewHolder> {

    private final List<Product> list;
    private final OnProductActionListener listener;
    private final ProductStatsDelegate statsDelegate;

    // 编辑/删除操作回调，由宿主 Activity 实现
    public interface OnProductActionListener {
        void onEdit(Product product);

        void onDelete(Product product, int position);
    }

    // 销量统计委托：适配器不直接查库，通过此接口向 Presenter 取数
    public interface ProductStatsDelegate {
        int getProductOrderCount(int productId);
        double getProductSalesRevenue(int productId);
    }

    public MyProductAdapter(List<Product> list, OnProductActionListener listener,
            ProductStatsDelegate statsDelegate) {
        this.list = list;
        this.listener = listener;
        this.statsDelegate = statsDelegate;
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
        holder.tvName.setText(p.isOffShelf() ? "［已下架］" + p.name : p.name);
        holder.tvDesc.setText(p.desc);
        holder.itemView.setAlpha(p.isOffShelf() ? 0.6f : 1f);
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

        int orderCount = statsDelegate.getProductOrderCount(p.id);
        double revenue = statsDelegate.getProductSalesRevenue(p.id);
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
