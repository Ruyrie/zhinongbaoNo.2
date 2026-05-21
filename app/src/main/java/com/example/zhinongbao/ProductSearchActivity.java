package com.example.zhinongbao;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ProductListAdapter;
import com.example.zhinongbao.adapter.PurchaseRequestAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Product;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.model.User;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSearchActivity extends AppCompatActivity {

    private static final int TAB_PRODUCTS = 0;
    private static final int TAB_PURCHASES = 1;
    private static final int TAB_STORES = 2;

    private DataManager dm;
    private List<Product> allProducts;
    private List<PurchaseRequest> allPurchaseRequests;
    private final List<Product> displayedProducts = new ArrayList<>();
    private final List<PurchaseRequest> displayedPurchaseRequests = new ArrayList<>();
    private final List<DataManager.StoreSearchResult> displayedStores = new ArrayList<>();
    private ProductListAdapter productAdapter;
    private PurchaseRequestAdapter purchaseAdapter;
    private StoreSearchAdapter storeAdapter;

    private EditText etSearchInput;
    private RecyclerView rvSearchResults;
    private View llSearchInitial;
    private View llSearchResults;
    private View llNoResults;
    private TextView tvNoResultsHint;
    private TextView tvClearSearch;
    private int currentTab = TAB_PRODUCTS;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        dm = DataManager.getInstance(this);
        allProducts = dm.getProducts();
        allPurchaseRequests = dm.getPurchaseRequests();

        ImageView ivBack = findViewById(R.id.ivBack);
        etSearchInput = findViewById(R.id.etSearchInput);
        TextView btnSearch = findViewById(R.id.btnSearch);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        rvSearchResults = findViewById(R.id.rvSearchResults);

        llSearchInitial = findViewById(R.id.llSearchInitial);
        llSearchResults = findViewById(R.id.llSearchResults);
        llNoResults = findViewById(R.id.llNoResults);
        tvNoResultsHint = findViewById(R.id.tvNoResultsHint);
        tvClearSearch = findViewById(R.id.tvClearSearch);

        ivBack.setOnClickListener(v -> finish());

        tabLayout.addTab(tabLayout.newTab().setText("售卖信息"));
        tabLayout.addTab(tabLayout.newTab().setText("采购信息"));
        tabLayout.addTab(tabLayout.newTab().setText("店铺"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshResults();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductListAdapter(displayedProducts, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.id);
            startActivity(intent);
        });
        purchaseAdapter = new PurchaseRequestAdapter(displayedPurchaseRequests, dm.getLoggedUser(),
                dm.getActiveRole() == User.ROLE_SELLER, new PurchaseRequestAdapter.OnActionListener() {
            @Override
            public void onQuoteClick(PurchaseRequest req) {
                openPurchaseMarket();
            }

            @Override
            public void onViewQuotesClick(PurchaseRequest req) {
                openPurchaseMarket();
            }

            @Override
            public void onItemClick(PurchaseRequest req) {
                openPurchaseMarket();
            }
        });
        storeAdapter = new StoreSearchAdapter(displayedStores, allProducts, store -> {
            Intent intent = new Intent(this, SellerStoreActivity.class);
            intent.putExtra("seller", store.seller);
            startActivity(intent);
        }, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.id);
            startActivity(intent);
        });
        rvSearchResults.setAdapter(productAdapter);

        btnSearch.setOnClickListener(v -> performSearch(etSearchInput.getText().toString().trim()));

        tvClearSearch.setOnClickListener(v -> {
            etSearchInput.setText("");
            showInitialState();
        });

        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() == 0) {
                    showInitialState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchInput.getText().toString().trim());
                return true;
            }
            return false;
        });

        wireTag(R.id.tagRice, "东北大米");
        wireTag(R.id.tagSmart, "有机黑木耳");
        wireTag(R.id.tagRural, "农家蜂蜜");
        wireTag(R.id.tagEco, "绿色蔬菜");
        wireTag(R.id.tagTech, "铁棍山药");
        wireTag(R.id.tagOrganic, "冬虫夏草");

        showInitialState();
    }

    private void wireTag(int viewId, String query) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(x -> {
                etSearchInput.setText(query);
                etSearchInput.setSelection(query.length());
                performSearch(query);
            });
        }
    }

    private void showInitialState() {
        currentQuery = "";
        llSearchInitial.setVisibility(View.VISIBLE);
        llSearchResults.setVisibility(View.GONE);
        llNoResults.setVisibility(View.GONE);

        etSearchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            showInitialState();
            return;
        }

        currentQuery = query;
        llSearchInitial.setVisibility(View.GONE);
        refreshResults();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearchInput.getWindowToken(), 0);
        }
    }

    private void refreshResults() {
        if (currentQuery.isEmpty()) {
            showInitialState();
            return;
        }

        if (currentTab == TAB_PRODUCTS) {
            searchProducts(currentQuery);
            rvSearchResults.setAdapter(productAdapter);
            productAdapter.notifyDataSetChanged();
            updateResultState(displayedProducts.isEmpty(), "未找到\"" + currentQuery + "\"的相关售卖信息");
        } else if (currentTab == TAB_PURCHASES) {
            searchPurchases(currentQuery);
            rvSearchResults.setAdapter(purchaseAdapter);
            purchaseAdapter.notifyDataSetChanged();
            updateResultState(displayedPurchaseRequests.isEmpty(), "未找到\"" + currentQuery + "\"的相关采购信息");
        } else {
            displayedStores.clear();
            displayedStores.addAll(dm.searchStores(currentQuery));
            storeAdapter.setQuery(currentQuery);
            rvSearchResults.setAdapter(storeAdapter);
            storeAdapter.notifyDataSetChanged();
            updateResultState(displayedStores.isEmpty(), "未找到\"" + currentQuery + "\"的相关店铺");
        }
    }

    private void searchProducts(String query) {
        displayedProducts.clear();
        for (Product p : allProducts) {
            if (contains(p.name, query) || contains(p.desc, query) || contains(p.category, query)) {
                displayedProducts.add(p);
            }
        }
    }

    private void searchPurchases(String query) {
        allPurchaseRequests = dm.getPurchaseRequests();
        displayedPurchaseRequests.clear();
        for (PurchaseRequest req : allPurchaseRequests) {
            if (contains(req.productName, query) || contains(req.description, query)
                    || contains(req.category, query) || contains(req.buyerNickname, query)) {
                displayedPurchaseRequests.add(req);
            }
        }
    }

    private boolean contains(String value, String query) {
        return value != null && query != null
                && value.toLowerCase().contains(query.toLowerCase());
    }

    private void updateResultState(boolean isEmpty, String emptyHint) {
        if (isEmpty) {
            llSearchResults.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
            llNoResults.setVisibility(View.VISIBLE);
            tvNoResultsHint.setText(emptyHint);
        } else {
            llSearchResults.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.VISIBLE);
            llNoResults.setVisibility(View.GONE);
        }
    }

    private void openPurchaseMarket() {
        startActivity(new Intent(this, PurchaseMarketActivity.class));
    }

    private static class StoreSearchAdapter extends RecyclerView.Adapter<StoreSearchAdapter.VH> {
        interface OnStoreClickListener {
            void onStoreClick(DataManager.StoreSearchResult store);
        }

        interface OnProductClickListener {
            void onProductClick(Product product);
        }

        private final List<DataManager.StoreSearchResult> stores;
        private final List<Product> products;
        private final OnStoreClickListener listener;
        private final OnProductClickListener productListener;
        private String query = "";

        StoreSearchAdapter(List<DataManager.StoreSearchResult> stores, List<Product> products,
                OnStoreClickListener listener, OnProductClickListener productListener) {
            this.stores = stores;
            this.products = products;
            this.listener = listener;
            this.productListener = productListener;
        }

        void setQuery(String query) {
            this.query = query == null ? "" : query;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_store, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DataManager.StoreSearchResult store = stores.get(position);
            holder.tvStoreName.setText(store.storeName);
            holder.tvStoreMeta.setText("账号：" + store.seller + "  商品：" + store.productCount + " 个");
            holder.tvStorePhone.setText(store.storePhone == null || store.storePhone.isEmpty()
                    ? "暂无联系电话"
                    : "电话：" + store.storePhone);
            bindMatchedProducts(holder, store.seller);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoreClick(store);
                }
            });
        }

        @Override
        public int getItemCount() {
            return stores.size();
        }

        private void bindMatchedProducts(VH holder, String seller) {
            List<Product> matchedProducts = getMatchedProducts(seller);
            holder.llStoreProducts.removeAllViews();
            if (matchedProducts.isEmpty()) {
                holder.tvStoreProducts.setText("店铺信息匹配");
                holder.llStoreProducts.setVisibility(View.GONE);
                return;
            }

            holder.tvStoreProducts.setText("相关商品");
            holder.llStoreProducts.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            for (Product product : matchedProducts) {
                View productView = inflater.inflate(R.layout.item_search_store_product,
                        holder.llStoreProducts, false);
                ImageView ivImage = productView.findViewById(R.id.ivStoreProductImage);
                TextView tvName = productView.findViewById(R.id.tvStoreProductName);
                TextView tvPrice = productView.findViewById(R.id.tvStoreProductPrice);
                tvName.setText(product.name);
                tvPrice.setText(String.format(Locale.getDefault(), "%.2f", product.price).replace(".00", ""));
                bindProductImage(ivImage, product);
                productView.setOnClickListener(v -> {
                    if (productListener != null) {
                        productListener.onProductClick(product);
                    }
                });
                holder.llStoreProducts.addView(productView);
            }
        }

        private List<Product> getMatchedProducts(String seller) {
            List<Product> matched = new ArrayList<>();
            for (Product product : products) {
                if (product.seller != null && product.seller.equals(seller)
                        && (matches(product.name) || matches(product.desc) || matches(product.category))) {
                    matched.add(product);
                }
            }
            return matched;
        }

        private void bindProductImage(ImageView imageView, Product product) {
            if (product.coverUri != null && !product.coverUri.isEmpty()) {
                if (product.coverUri.startsWith("res://")) {
                    int resId = Integer.parseInt(product.coverUri.replace("res://", ""));
                    imageView.setImageResource(resId);
                } else {
                    imageView.setImageURI(Uri.parse(product.coverUri));
                }
                return;
            }

            switch (product.id) {
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
                    imageView.setImageResource(R.drawable.ic_product_placeholder);
                    break;
            }
        }

        private boolean matches(String value) {
            return value != null && !query.isEmpty()
                    && value.toLowerCase().contains(query.toLowerCase());
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvStoreName, tvStoreMeta, tvStorePhone, tvStoreProducts;
            LinearLayout llStoreProducts;

            VH(View view) {
                super(view);
                tvStoreName = view.findViewById(R.id.tvSearchStoreName);
                tvStoreMeta = view.findViewById(R.id.tvSearchStoreMeta);
                tvStorePhone = view.findViewById(R.id.tvSearchStorePhone);
                tvStoreProducts = view.findViewById(R.id.tvSearchStoreProducts);
                llStoreProducts = view.findViewById(R.id.llSearchStoreProducts);
            }
        }
    }
}
