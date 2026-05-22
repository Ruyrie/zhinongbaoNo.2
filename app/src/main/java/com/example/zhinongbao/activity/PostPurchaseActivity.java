package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.postpurchase.PostPurchaseContract;
import com.example.zhinongbao.mvp.postpurchase.PostPurchasePresenter;

public class PostPurchaseActivity extends BaseMvpActivity<PostPurchaseContract.Presenter>
        implements PostPurchaseContract.View {

    private EditText etProductName, etCategory, etQuantity, etUnit, etTargetPrice, etDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_purchase);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        new PostPurchasePresenter(this, this);

        etProductName = findViewById(R.id.etPurchaseProductName);
        etCategory = findViewById(R.id.etPurchaseCategory);
        etQuantity = findViewById(R.id.etPurchaseQuantity);
        etUnit = findViewById(R.id.etPurchaseUnit);
        etTargetPrice = findViewById(R.id.etPurchaseTargetPrice);
        etDesc = findViewById(R.id.etPurchaseDesc);

        findViewById(R.id.ivPostPurchaseBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitPurchase).setOnClickListener(v -> submit());
    }

    private void submit() {
        String name = etProductName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();
        String priceStr = etTargetPrice.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        presenter.submit(name, category, qtyStr, unit, priceStr, desc);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void closePage() {
        finish();
    }
}
