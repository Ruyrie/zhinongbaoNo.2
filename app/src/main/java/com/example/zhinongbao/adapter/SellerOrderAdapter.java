package com.example.zhinongbao.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.R;
import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.Product;
import java.util.List;

/**
 * ============================================================
 * 【卖家订单 / Seller Order】Adapter（RecyclerView 适配器）
 * 整体逻辑：按 status 切换状态文字与颜色，并决定按钮——
 *   待付款→修改价格/联系买家；待发货→去发货；已发货→联系买家；待处理售后→处理售后。
 *   封面图：采购订单优先展示买家上传的需求图片，其次内置示例图，再次商品封面，最后占位图。
 * 数据来源：构造时传入的 List<Order>；商品信息通过 ProductResolver 回调宿主向 Presenter 取。
 * 配合的文件：模型 model/Order、model/Product；行布局 res/layout/item_seller_order.xml；
 *   宿主 activity/SellerOrdersActivity（实现操作回调）。
 * 提示：在 IDE 里搜索「卖家订单」可看本组相关文件。
 * ============================================================
 */
public class SellerOrderAdapter extends RecyclerView.Adapter<SellerOrderAdapter.VH> {

    private final List<Order> list;
    private final OnOrderActionListener listener;
    private final ProductResolver productResolver;

    public interface OnOrderActionListener {
        void onShip(Order o);

        void onModifyPrice(Order o);

        void onRefund(Order o);

        void onContactBuyer(Order o);

        // 点击订单卡片（商品信息区）→ 进入该商品详情页
        void onOrderClick(Order o);
    }

    // 商品解析委托：适配器不直接查库，通过此接口向 Presenter 取商品
    public interface ProductResolver {
        Product getProductById(int productId);
    }

    public SellerOrderAdapter(List<Order> list, OnOrderActionListener listener, ProductResolver productResolver) {
        this.list = list;
        this.listener = listener;
        this.productResolver = productResolver;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seller_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Order o = list.get(position);
        // 点击整张卡片进入商品详情（操作按钮各自消费点击，不会触发此处）
        holder.itemView.setOnClickListener(v -> listener.onOrderClick(o));
        holder.tvOrderId.setText("订单号: " + o.orderId);

        if (Order.ORDER_TYPE_PROCUREMENT.equals(o.orderType)) {
            holder.tvOrderType.setText("供货订单");
            holder.tvOrderType.setTextColor(0xFFFF9800);
        } else {
            holder.tvOrderType.setText("零售订单");
            holder.tvOrderType.setTextColor(0xFF007AFF);
        }

        Product p = productResolver.getProductById(o.productId);
        int defaultRes = getDefaultProductImageRes(o.productId);
        String orderImage = firstImage(o.proofImages); // 采购订单展示买家上传的需求图片
        if (Order.ORDER_TYPE_PROCUREMENT.equals(o.orderType) && orderImage != null) {
            holder.ivCover.setImageURI(android.net.Uri.parse(orderImage));
        } else if (defaultRes != 0) {
            holder.ivCover.setImageResource(defaultRes);
        } else if (p != null && p.coverUri != null && !p.coverUri.isEmpty()) {
            holder.ivCover.setImageURI(android.net.Uri.parse(p.coverUri.split(",")[0]));
        } else if (orderImage != null) {
            holder.ivCover.setImageURI(android.net.Uri.parse(orderImage));
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_product_placeholder);
        }

        holder.tvProductName.setText(o.name);
        holder.tvProductQuantity.setText("x" + o.quantity);

        double currentUnitPrice = o.unitPrice > 0 ? o.unitPrice : o.price;
        holder.tvProductPrice.setText(String.format("¥%.2f", currentUnitPrice));

        double total = currentUnitPrice * o.quantity - o.discount;
        holder.tvTotalPrice.setText(String.format("实付: ¥%.2f", total));

        holder.tvBuyerInfo.setText("买家: " + o.buyerNickname);
        holder.tvOrderTime.setText("下单时间: " + o.time);

        holder.btnAction1.setVisibility(View.GONE);
        holder.btnAction2.setVisibility(View.GONE);

        switch (o.status) {
            case Order.STATUS_PENDING:
                holder.tvOrderStatus.setText("待付款");
                holder.tvOrderStatus.setTextColor(0xFFFF9800);
                holder.btnAction1.setVisibility(View.VISIBLE);
                holder.btnAction1.setText("修改价格");
                holder.btnAction1.setOnClickListener(v -> listener.onModifyPrice(o));
                holder.btnAction2.setVisibility(View.VISIBLE);
                holder.btnAction2.setText("联系买家");
                holder.btnAction2.setOnClickListener(v -> listener.onContactBuyer(o));
                break;
            case Order.STATUS_PAID:
                holder.tvOrderStatus.setText("待发货");
                holder.tvOrderStatus.setTextColor(0xFF007AFF);
                holder.btnAction2.setVisibility(View.VISIBLE);
                holder.btnAction2.setText("去发货");
                holder.btnAction2.setOnClickListener(v -> listener.onShip(o));
                break;
            case Order.STATUS_SHIPPED:
                holder.tvOrderStatus.setText("已发货");
                holder.tvOrderStatus.setTextColor(0xFF4CAF50);
                if (!TextUtils.isEmpty(o.shipName)) {
                    holder.tvOrderTime.append("\n物流: " + o.shipName + " " + o.shipNo);
                }
                holder.btnAction2.setVisibility(View.VISIBLE);
                holder.btnAction2.setText("联系买家");
                holder.btnAction2.setOnClickListener(v -> listener.onContactBuyer(o));
                break;
            case Order.STATUS_COMPLETED:
                holder.tvOrderStatus.setText(o.refundAmount > 0 ? "已退款" : "已完成");
                holder.tvOrderStatus.setTextColor(0xFF4CAF50);
                if (o.refundAmount > 0) {
                    holder.tvOrderTime.append(String.format("\n已退款: ¥%.2f", o.refundAmount));
                }
                break;
            case Order.STATUS_REFUND:
                holder.tvOrderStatus.setText("待处理售后");
                holder.tvOrderStatus.setTextColor(0xFFF44336);
                if (o.refundAmount > 0) {
                    holder.tvOrderTime.append(String.format("\n申请退款: ¥%.2f\n原因: %s", o.refundAmount, o.refundReason));
                    if (o.refundRequestedAt > 0) {
                        long remain = o.refundRequestedAt + 24L * 60 * 60 * 1000 - System.currentTimeMillis();
                        if (remain > 0) {
                            long h = remain / 3600000;
                            long m = (remain % 3600000) / 60000;
                            holder.tvOrderTime.append(String.format("\n自动退款倒计时: %02d小时%02d分", h, m));
                        }
                    }
                }
                holder.btnAction2.setVisibility(View.VISIBLE);
                holder.btnAction2.setText("处理售后");
                holder.btnAction2.setOnClickListener(v -> listener.onRefund(o));
                break;
            case Order.STATUS_CANCELLED:
                holder.tvOrderStatus.setText("已取消");
                holder.tvOrderStatus.setTextColor(0xFF999999);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // 取逗号分隔图片串里的第一张，空则返回 null
    private String firstImage(String images) {
        if (TextUtils.isEmpty(images)) {
            return null;
        }
        String first = images.split(",")[0].trim();
        return first.isEmpty() ? null : first;
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

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderType, tvOrderId, tvOrderStatus, tvProductName, tvProductPrice, tvProductQuantity;
        TextView tvBuyerInfo, tvTotalPrice, tvOrderTime;
        ImageView ivCover;
        TextView btnAction1, btnAction2;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvOrderType = itemView.findViewById(R.id.tvOrderType);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            ivCover = itemView.findViewById(R.id.ivProductCover);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvProductQuantity = itemView.findViewById(R.id.tvProductQuantity);
            tvBuyerInfo = itemView.findViewById(R.id.tvBuyerInfo);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            btnAction1 = itemView.findViewById(R.id.btnAction1);
            btnAction2 = itemView.findViewById(R.id.btnAction2);
        }
    }
}
