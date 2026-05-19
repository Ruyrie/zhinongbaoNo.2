package com.example.zhinongbao;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zhinongbao.data.DataManager;

public class PostPurchaseActivity extends AppCompatActivity {

    private EditText etProductName, etCategory, etQuantity, etUnit, etTargetPrice, etDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_purchase);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

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

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "请填写货品名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(qtyStr)) {
            Toast.makeText(this, "请填写需求数量", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(unit)) {
            Toast.makeText(this, "请填写单位", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "请填写目标价格", Toast.LENGTH_SHORT).show();
            return;
        }

        double quantity, targetPrice;
        try {
            quantity = Double.parseDouble(qtyStr);
            targetPrice = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "数量或价格格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean ok = DataManager.getInstance(this)
                .addPurchaseRequest(name, category, quantity, unit, targetPrice, desc);
        if (ok) {
            Toast.makeText(this, "采购需求发布成功！", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "发布失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
}
