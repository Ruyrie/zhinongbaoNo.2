package com.example.zhinongbao.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.CartItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface OnChangeListener {
        void onChange();
    }

    private final List<CartItem> data;
    private final Set<Integer> checkedIds = new HashSet<>(); // use productId as key
    private OnChangeListener listener;

    public CartAdapter(List<CartItem> data) {
        this.data = data;
    }

    public void setOnChangeListener(OnChangeListener l) {
        this.listener = l;
    }

    public void setAllChecked(boolean val) {
        checkedIds.clear();
        if (val) {
            for (CartItem c : data)
                checkedIds.add(c.productId);
        }
        notifyDataSetChanged();
        if (listener != null)
            listener.onChange();
    }

    public boolean areAllChecked() {
        if (data.isEmpty())
            return false;
        for (CartItem c : data)
            if (!checkedIds.contains(c.productId))
                return false;
        return true;
    }

    public double getSelectedTotal() {
        double total = 0;
        for (CartItem c : data)
            if (checkedIds.contains(c.productId))
                total += c.price * c.quantity;
        return total;
    }

    /** Return a snapshot of currently checked items */
    public List<CartItem> getCheckedItems() {
        List<CartItem> result = new ArrayList<>();
        for (CartItem c : data)
            if (checkedIds.contains(c.productId))
                result.add(c);
        return result;
    }

    /** Remove checked items, return count */
    public int removeChecked() {
        int before = data.size();
        data.removeIf(c -> checkedIds.contains(c.productId));
        checkedIds.clear();
        notifyDataSetChanged();
        return before - data.size();
    }

    /** Remove ALL items */
    public void clearAll() {
        data.clear();
        checkedIds.clear();
        notifyDataSetChanged();
        if (listener != null)
            listener.onChange();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CartItem c = data.get(position);
        holder.tvName.setText(c.name);
        holder.tvPrice.setText(String.format("¥%.2f", c.price));
        holder.tvQty.setText(String.valueOf(c.quantity));

        switch (c.productId) {
            case 1:
                holder.ivImage.setImageResource(R.mipmap.dami1);
                break;
            case 2:
                holder.ivImage.setImageResource(R.mipmap.muer);
                break;
            case 3:
                holder.ivImage.setImageResource(R.mipmap.fengmi1);
                break;
            case 4:
                holder.ivImage.setImageResource(R.mipmap.shucai1);
                break;
            case 5:
                holder.ivImage.setImageResource(R.mipmap.dongchongxiacao1);
                break;
            case 6:
                holder.ivImage.setImageResource(R.mipmap.hongshu1);
                break;
            case 7:
                holder.ivImage.setImageResource(R.mipmap.shanyao1);
                break;
            case 8:
                holder.ivImage.setImageResource(R.mipmap.yangdujun1);
                break;
            case 9:
                holder.ivImage.setImageResource(R.mipmap.luronggu1);
                break;
            case 10:
                holder.ivImage.setImageResource(R.mipmap.tuedan1);
                break;
            default:
                holder.ivImage.setImageResource(R.drawable.ic_product_placeholder);
        }

        holder.cbItem.setOnCheckedChangeListener(null);
        holder.cbItem.setChecked(checkedIds.contains(c.productId));
        holder.cbItem.setOnCheckedChangeListener((btn, checked) -> {
            if (checked)
                checkedIds.add(data.get(holder.getAdapterPosition()).productId);
            else
                checkedIds.remove(data.get(holder.getAdapterPosition()).productId);
            if (listener != null)
                listener.onChange();
        });

        holder.btnMinus.setOnClickListener(v -> {
            CartItem item = data.get(holder.getAdapterPosition());
            if (item.quantity > 1) {
                item.quantity--;
                notifyItemChanged(holder.getAdapterPosition());
            } else {
                checkedIds.remove(item.productId);
                data.remove(holder.getAdapterPosition());
                notifyDataSetChanged();
            }
            if (listener != null)
                listener.onChange();
        });

        holder.btnPlus.setOnClickListener(v -> {
            data.get(holder.getAdapterPosition()).quantity++;
            notifyItemChanged(holder.getAdapterPosition());
            if (listener != null)
                listener.onChange();
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbItem;
        ImageView ivImage;
        TextView tvName, tvPrice, tvQty, btnMinus, btnPlus;

        VH(View v) {
            super(v);
            cbItem = v.findViewById(R.id.cbCartItem);
            ivImage = v.findViewById(R.id.ivCartImage);
            tvName = v.findViewById(R.id.tvCartName);
            tvPrice = v.findViewById(R.id.tvCartPrice);
            tvQty = v.findViewById(R.id.tvCartQty);
            btnMinus = v.findViewById(R.id.btnMinus);
            btnPlus = v.findViewById(R.id.btnPlus);
        }
    }
}
