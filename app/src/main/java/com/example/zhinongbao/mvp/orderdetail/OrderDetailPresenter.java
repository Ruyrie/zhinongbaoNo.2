package com.example.zhinongbao.mvp.orderdetail;

import android.content.Context;

import com.example.zhinongbao.model.Order;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.repository.OrderRepository;
import com.example.zhinongbao.repository.ProductRepository;
import com.example.zhinongbao.repository.PurchaseRepository;
import com.example.zhinongbao.repository.UserRepository;

public class OrderDetailPresenter implements OrderDetailContract.Presenter {
    private final OrderDetailContract.View view;
    private final OrderRepository repository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final String orderId;
    private final String username;
    private Order order;

    public OrderDetailPresenter(Context context, OrderDetailContract.View view, String orderId) {
        this.view = view;
        this.orderId = orderId;
        this.repository = new OrderRepository(context.getApplicationContext());
        this.productRepository = new ProductRepository(context.getApplicationContext());
        this.userRepository = new UserRepository(context.getApplicationContext());
        this.purchaseRepository = new PurchaseRepository(context.getApplicationContext());
        this.username = repository.getLoggedUser();
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        order = repository.getOrderById(orderId);
        if (order == null) {
            view.closePage();
            return;
        }

        if (Order.STATUS_PENDING.equals(order.status) && order.getRemainingMs() <= 0) {
            repository.updateOrderStatus(username, order.orderId, Order.STATUS_CANCELLED);
            order.status = Order.STATUS_CANCELLED;
        }

        // 采购订单：订单自带的图片为空时（如旧订单），回源到买家发布的需求图片，避免详情页只剩占位图
        if ((order.proofImages == null || order.proofImages.trim().isEmpty())
                && Order.ORDER_TYPE_PROCUREMENT.equals(order.orderType)) {
            String requestImages = purchaseRepository.getRequestImages(order.purchaseRequestId);
            if (requestImages != null && !requestImages.trim().isEmpty()) {
                order.proofImages = requestImages;
            }
        }

        boolean canComment = order.productId > 0 && !Order.ORDER_TYPE_PROCUREMENT.equals(order.orderType);
        String storeName = order.seller == null || order.seller.isEmpty() ? "" : userRepository.getStoreName(order.seller);
        String storePhone = order.seller == null || order.seller.isEmpty() ? "" : productRepository.getStorePhone(order.seller);
        String storeAvatarUri = order.seller == null || order.seller.isEmpty() ? "" : userRepository.getAvatarUri(order.seller);
        view.showOrder(order, repository.getOrderPaidAmount(order), canComment, repository.canRequestRefund(order),
                storeName, storePhone, storeAvatarUri);
    }

    @Override
    public void onPrimaryAction() {
        if (order == null) {
            return;
        }

        if (Order.STATUS_SHIPPED.equals(order.status)) {
            repository.confirmReceipt(username, order.orderId);
            // 采购订单（无真实商品）不支持评价，提示语不带「评价」
            boolean reviewable = order.productId > 0 && !Order.ORDER_TYPE_PROCUREMENT.equals(order.orderType);
            view.showToast(reviewable ? "已确认收货，现在可以评价商品" : "已确认收货");
            view.closePage();
        } else if (Order.STATUS_PAID.equals(order.status)) {
            view.showRefundDialog();
        } else if (Order.STATUS_PENDING.equals(order.status)) {
            repository.updateOrderStatus(username, order.orderId, Order.STATUS_PAID);
            view.showToast("支付成功！");
            view.closePage();
        }
    }

    @Override
    public void onSecondaryAction() {
        if (order == null) {
            return;
        }

        if (Order.STATUS_SHIPPED.equals(order.status)) {
            view.showRefundDialog();
        } else if (Order.STATUS_COMPLETED.equals(order.status)) {
            if (repository.canRequestRefund(order)) {
                view.showRefundDialog();
            } else {
                view.openStoreChat();
            }
        } else {
            view.showCancelConfirm();
        }
    }

    @Override
    public void confirmCancel() {
        if (order == null) {
            return;
        }

        repository.updateOrderStatus(username, order.orderId, Order.STATUS_CANCELLED);
        view.showToast("订单已取消");
        view.closePage();
    }

    @Override
    public void requestRefund(String reason) {
        if (order == null) {
            return;
        }

        String finalReason = reason == null || reason.trim().isEmpty() ? "买家申请退款" : reason.trim();
        if (repository.initiateRefund(order.orderId, finalReason)) {
            view.showToast("退款申请已提交");
            view.closePage();
        } else {
            view.showToast("当前订单已超过退款时限，请联系客服");
        }
    }

    @Override
    public void addToCart() {
        if (username == null || username.isEmpty()) {
            view.showToast("请先登录");
            return;
        }
        if (order == null || order.productId <= 0) {
            view.showToast("当前商品无法加入购物车");
            return;
        }
        Product product = productRepository.getProductById(order.productId);
        if (product == null) {
            view.showToast("商品已下架");
            return;
        }
        productRepository.addToCart(username, product);
        view.showToast("已加入购物车");
    }
}
