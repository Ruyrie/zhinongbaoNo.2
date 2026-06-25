package com.example.zhinongbao.adapter;

/* ============================================================
 * 【购物车 / Cart】列表适配器（Adapter，把数据画到列表上）
 * ============================================================
 * 这个文件是干什么的：
 *   RecyclerView（列表）本身不知道每行该显示什么，Adapter 就是那个「翻译官」：
 *   它把一条条 CartItem 数据，按照 item_cart.xml 的样子，画成屏幕上的一行行。
 *   同时它还管理「哪些行被勾选了、加减数量、删除某行」。
 *
 * 适配器三个核心方法（每个 RecyclerView.Adapter 都有）：
 *   - onCreateViewHolder：创建一个空白的「行」（把 xml 充气成 View）。
 *   - onBindViewHolder：把第 position 条数据填进这一行（设置文字、图片、按钮）。
 *   - getItemCount：告诉列表一共有多少行。
 *   ViewHolder（内部类 VH）：缓存一行里的各个控件，避免反复 findViewById 拖慢速度。
 *
 * 配套：CartActivity 创建并使用它；item_cart.xml 是单行布局；CartItem 是单行数据。
 * 提示：在 IDE 里搜索「购物车」可看本组全部文件。
 * ============================================================ */

import android.net.Uri;
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

    // 回调接口：列表内容一变化（勾选/加减/删除），就通知外面（CartActivity）刷新底部栏
    public interface OnChangeListener {
        void onChange();
    }

    private final List<CartItem> data;                       // 列表数据（和 CartActivity 共用同一个 List）
    private final Set<Integer> checkedIds = new HashSet<>(); // 记录被勾选商品的 id（用 Set 自动去重）
    private OnChangeListener listener;                       // 变化回调（指向 CartActivity）

    public CartAdapter(List<CartItem> data) {
        this.data = data;
    }

    public void setOnChangeListener(OnChangeListener l) {
        this.listener = l;
    }

    // 全选/全不选：val=true 就把所有商品 id 都加入勾选集合，否则清空
    public void setAllChecked(boolean val) {
        checkedIds.clear();
        if (val) {
            for (CartItem c : data)
                checkedIds.add(c.productId);
        }
        notifyDataSetChanged();                              // 通知列表重画（勾选框状态变了）
        if (listener != null)
            listener.onChange();
    }

    // 判断是否「全部已勾选」：空购物车算 false；只要有一个没勾就 false
    public boolean areAllChecked() {
        if (data.isEmpty())
            return false;
        for (CartItem c : data)
            if (!checkedIds.contains(c.productId))
                return false;
        return true;
    }

    // 计算「已勾选商品」的合计金额 = 累加每件的 单价 × 数量
    public double getSelectedTotal() {
        double total = 0;
        for (CartItem c : data)
            if (checkedIds.contains(c.productId))
                total += c.price * c.quantity;
        return total;
    }

    /** 返回当前所有「已勾选」的商品（结算时用） */
    public List<CartItem> getCheckedItems() {
        List<CartItem> result = new ArrayList<>();
        for (CartItem c : data)
            if (checkedIds.contains(c.productId))
                result.add(c);
        return result;
    }

    /** 删除所有「已勾选」的商品，返回删掉了几件 */
    public int removeChecked() {
        int before = data.size();
        data.removeIf(c -> checkedIds.contains(c.productId)); // removeIf：满足条件的元素就删掉
        checkedIds.clear();
        notifyDataSetChanged();
        return before - data.size();                          // 原数量 - 现数量 = 删除数量
    }

    /** 清空全部商品 */
    public void clearAll() {
        data.clear();
        checkedIds.clear();
        notifyDataSetChanged();
        if (listener != null)
            listener.onChange();
    }

    // onCreateViewHolder：创建一行的「空壳」。LayoutInflater 把 item_cart.xml 变成真正的 View。
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    // onBindViewHolder：把第 position 条数据填进这一行（设置名称、价格、数量、图片、按钮）
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CartItem c = data.get(position);                      // 取出这一行对应的数据
        holder.tvName.setText(c.name);                        // 显示商品名
        holder.tvPrice.setText(String.format("¥%.2f", c.price)); // 显示单价（两位小数）
        holder.tvQty.setText(String.valueOf(c.quantity));     // 显示数量

        // 先尝试用商品自带封面图；成功就直接绑定按钮后返回
        if (bindCoverImage(holder.ivImage, c.coverUri)) {
            bindControls(holder, c);
            return;
        }
        // 没有封面图就用「内置兜底图」（按商品 id 配一张默认图片）
        bindFallbackImage(holder.ivImage, c.productId);
        bindControls(holder, c);
    }

    // 尝试加载商品封面图。返回 true=已处理图片，false=没有可用封面（外层会改用兜底图）
    private boolean bindCoverImage(ImageView imageView, String coverUri) {
        if (coverUri == null || coverUri.trim().isEmpty()) {
            return false;                                     // 没填封面 → 交给兜底图
        }
        // 封面可能是「逗号分隔的多张图」，这里只取第一张
        String firstUri = coverUri.contains(",") ? coverUri.split(",")[0].trim() : coverUri.trim();
        if (firstUri.isEmpty()) {
            return false;
        }
        try {
            if (firstUri.startsWith("res://")) {
                // res:// 开头 = 项目内置图片资源，取出资源 id 直接显示
                imageView.setImageResource(Integer.parseInt(firstUri.replace("res://", "")));
            } else {
                // 否则当作手机里的图片地址（Uri），先放占位图再加载
                imageView.setImageResource(R.drawable.ic_product_placeholder);
                imageView.setImageURI(Uri.parse(firstUri));
            }
            return true;
        } catch (Exception e) {
            // 加载出错（地址非法等）就显示占位图，避免崩溃
            imageView.setImageResource(R.drawable.ic_product_placeholder);
            return true;
        }
    }

    // 兜底图：给项目自带的 1~10 号示例商品，各配一张内置图片
    private void bindFallbackImage(ImageView imageView, int productId) {
        switch (productId) {
            case 1:
                imageView.setImageResource(R.mipmap.dami1);
                break;
            case 2:
                imageView.setImageResource(R.mipmap.muer);
                break;
            case 3:
                imageView.setImageResource(R.mipmap.fengmi1);
                break;
            case 4:
                imageView.setImageResource(R.mipmap.shucai1);
                break;
            case 5:
                imageView.setImageResource(R.mipmap.dongchongxiacao1);
                break;
            case 6:
                imageView.setImageResource(R.mipmap.hongshu1);
                break;
            case 7:
                imageView.setImageResource(R.mipmap.shanyao1);
                break;
            case 8:
                imageView.setImageResource(R.mipmap.yangdujun1);
                break;
            case 9:
                imageView.setImageResource(R.mipmap.luronggu1);
                break;
            case 10:
                imageView.setImageResource(R.mipmap.tuedan1);
                break;
            default:
                imageView.setImageResource(R.drawable.ic_product_placeholder); // 其它商品用通用占位图
        }
    }

    // 给这一行的「勾选框、减号、加号」三个控件绑定点击逻辑
    private void bindControls(@NonNull VH holder, CartItem c) {
        // 勾选框：先清监听器再设状态，防止「代码设置勾选」误触发监听导致逻辑混乱
        holder.cbItem.setOnCheckedChangeListener(null);
        holder.cbItem.setChecked(checkedIds.contains(c.productId));
        holder.cbItem.setOnCheckedChangeListener((btn, checked) -> {
            if (checked)
                checkedIds.add(data.get(holder.getAdapterPosition()).productId);   // 勾上→记入已选集合
            else
                checkedIds.remove(data.get(holder.getAdapterPosition()).productId); // 取消→移出已选集合
            if (listener != null)
                listener.onChange();                          // 通知外面刷新底部合计
        });

        // 减号按钮：数量>1 就减一；等于1 再减就把这件商品从购物车删除
        holder.btnMinus.setOnClickListener(v -> {
            CartItem item = data.get(holder.getAdapterPosition());
            if (item.quantity > 1) {
                item.quantity--;
                notifyItemChanged(holder.getAdapterPosition()); // 只刷新这一行，效率高
            } else {
                checkedIds.remove(item.productId);
                data.remove(holder.getAdapterPosition());
                notifyDataSetChanged();                        // 删除了行，整体刷新
            }
            if (listener != null)
                listener.onChange();
        });

        // 加号按钮：数量 +1
        holder.btnPlus.setOnClickListener(v -> {
            data.get(holder.getAdapterPosition()).quantity++;
            notifyItemChanged(holder.getAdapterPosition());
            if (listener != null)
                listener.onChange();
        });
    }

    // getItemCount：列表一共多少行 = 数据条数
    @Override
    public int getItemCount() {
        return data.size();
    }

    // VH（ViewHolder）：缓存一行里的各个控件引用，避免每次绑定都重新 findViewById（提速）
    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbItem;                                      // 勾选框
        ImageView ivImage;                                   // 商品图片
        TextView tvName, tvPrice, tvQty, btnMinus, btnPlus;  // 名称、价格、数量、减号、加号

        VH(View v) {
            super(v);
            // 从这一行的布局里把各控件按 id 取出来，存好备用
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
