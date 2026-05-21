package com.example.zhinongbao.mvp.productsearch;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.model.StoreSearchResult;

import java.util.List;

public interface ProductSearchContract {
    interface View extends BaseView<Presenter> {
        void showInitialData(List<Product> products, List<PurchaseRequest> purchaseRequests, String currentUser,
                boolean sellerMode);
        void showStoreResults(List<StoreSearchResult> stores);
    }

    interface Presenter extends BasePresenter {
        void searchStores(String query);
        List<PurchaseRequest> getPurchaseRequests();
    }
}
