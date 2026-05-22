package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.PurchaseRequest;
import com.example.zhinongbao.mvp.postpurchase.PostPurchaseContract;
import com.example.zhinongbao.mvp.postpurchase.PostPurchasePresenter;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

public class PostPurchaseActivity extends BaseMvpActivity<PostPurchaseContract.Presenter>
        implements PostPurchaseContract.View {

    private EditText etProductName, etCategory, etQuantity, etUnit, etTargetPrice, etDesc;
    private TextView tvTotal;
    private boolean formattingPrice;
    private long editRequestId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_purchase);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        new PostPurchasePresenter(this, this);
        editRequestId = getIntent().getLongExtra("request_id", -1);

        etProductName = findViewById(R.id.etPurchaseProductName);
        etCategory = findViewById(R.id.etPurchaseCategory);
        etQuantity = findViewById(R.id.etPurchaseQuantity);
        etUnit = findViewById(R.id.etPurchaseUnit);
        etTargetPrice = findViewById(R.id.etPurchaseTargetPrice);
        etDesc = findViewById(R.id.etPurchaseDesc);
        tvTotal = findViewById(R.id.tvPurchaseTotal);
        bindAmountInputs();

        findViewById(R.id.ivPostPurchaseBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitPurchase).setOnClickListener(v -> submit());
        if (editRequestId > 0) {
            ((TextView) findViewById(R.id.btnSubmitPurchase)).setText("保存");
            presenter.loadRequest(editRequestId);
        }
    }

    private void submit() {
        String name = etProductName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();
        String priceStr = cleanNumber(etTargetPrice.getText().toString().trim());
        String desc = etDesc.getText().toString().trim();

        if (editRequestId > 0) {
            presenter.submitEdit(editRequestId, name, category, qtyStr, unit, priceStr, desc);
        } else {
            presenter.submit(name, category, qtyStr, unit, priceStr, desc);
        }
    }

    @Override
    public void showExistingRequest(PurchaseRequest request) {
        etProductName.setText(request.productName);
        etCategory.setText(request.category);
        etQuantity.setText(formatQty(request.quantity));
        etUnit.setText(request.unit);
        etTargetPrice.setText(formatEditableNumber(formatPlain(request.targetPrice)));
        etDesc.setText(request.description == null ? "" : request.description);
        updateTotal();
    }

    private void bindAmountInputs() {
        TextWatcher totalWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateTotal();
            }
        };
        etQuantity.addTextChangedListener(totalWatcher);
        etTargetPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!formattingPrice) {
                    formatPriceInput(s);
                }
                updateTotal();
            }
        });
    }

    private void formatPriceInput(Editable s) {
        String raw = cleanNumber(s.toString());
        if (raw.isEmpty() || ".".equals(raw)) {
            return;
        }
        try {
            formattingPrice = true;
            String formatted = formatEditableNumber(raw);
            etTargetPrice.setText(formatted);
            etTargetPrice.setSelection(formatted.length());
        } finally {
            formattingPrice = false;
        }
    }

    private String formatEditableNumber(String raw) {
        int dotIndex = raw.indexOf('.');
        String integerPart = dotIndex >= 0 ? raw.substring(0, dotIndex) : raw;
        String decimalPart = dotIndex >= 0 ? raw.substring(dotIndex) : "";
        if (integerPart.isEmpty()) {
            return decimalPart.isEmpty() ? "" : "0" + decimalPart;
        }
        return groupInteger(integerPart) + decimalPart;
    }

    private String groupInteger(String value) {
        BigInteger integer = new BigInteger(value);
        String digits = integer.toString();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                builder.append(',');
            }
            builder.append(digits.charAt(i));
        }
        return builder.toString();
    }

    private void updateTotal() {
        try {
            double qty = Double.parseDouble(cleanNumber(etQuantity.getText().toString()));
            double price = Double.parseDouble(cleanNumber(etTargetPrice.getText().toString()));
            tvTotal.setText("¥" + formatMoney(qty * price));
        } catch (Exception e) {
            tvTotal.setText("¥0.00");
        }
    }

    private String cleanNumber(String value) {
        return value == null ? "" : value.replace(",", "").replace("¥", "").trim();
    }

    private String formatQty(double value) {
        return value % 1 == 0 ? String.valueOf((int) value) : String.valueOf(value);
    }

    private String formatPlain(double value) {
        return value % 1 == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    private String formatMoney(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.CHINA);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
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
