package com.example.zhinongbao;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ProductListAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.Product;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class ProductSearchActivity extends AppCompatActivity {

    private List<Product> allProducts;
    private List<Product> displayed;
    private ProductListAdapter adapter;

    private EditText etSearchInput;
    private RecyclerView rvSearchResults;
    private View llSearchInitial;
    private View llSearchResults;
    private View llNoResults;
    private TextView tvNoResultsHint;
    private TextView tvClearSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_search);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        DataManager dm = DataManager.getInstance(this);
        allProducts = dm.getProducts();
        displayed = new ArrayList<>();

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

        // Setup Tabs
        tabLayout.addTab(tabLayout.newTab().setText("售卖信息"));
        tabLayout.addTab(tabLayout.newTab().setText("采购信息"));
        tabLayout.addTab(tabLayout.newTab().setText("店铺"));

        // Setup RecyclerView
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductListAdapter(displayed, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.id);
            startActivity(intent);
        });
        rvSearchResults.setAdapter(adapter);

        // Setup Search Actions
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

        // Hot tags
        wireTag(R.id.tagRice, "东北大米");
        wireTag(R.id.tagSmart, "有机黑木耳");
        wireTag(R.id.tagRural, "农家蜂蜜");
        wireTag(R.id.tagEco, "绿色蔬菜");
        wireTag(R.id.tagTech, "铁棍山药");
        wireTag(R.id.tagOrganic, "冬虫夏草");

        // Request focus and show keyboard
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

        llSearchInitial.setVisibility(View.GONE);
        displayed.clear();
        for (Product p : allProducts) {
            if (p.name.contains(query) || p.desc.contains(query)) {
                displayed.add(p);
            }
        }
        adapter.notifyDataSetChanged();

        if (displayed.isEmpty()) {
            llSearchResults.setVisibility(View.GONE);
            llNoResults.setVisibility(View.VISIBLE);
            tvNoResultsHint.setText("未找到\"" + query + "\"的相关商品");
        } else {
            llSearchResults.setVisibility(View.VISIBLE);
            llNoResults.setVisibility(View.GONE);
        }

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearchInput.getWindowToken(), 0);
        }
    }
}
