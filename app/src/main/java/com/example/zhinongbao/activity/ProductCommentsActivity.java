package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ProductCommentAdapter;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.model.ProductComment;
import com.example.zhinongbao.mvp.productcomments.ProductCommentsContract;
import com.example.zhinongbao.mvp.productcomments.ProductCommentsPresenter;
import java.util.List;

public class ProductCommentsActivity extends BaseMvpActivity<ProductCommentsContract.Presenter>
        implements ProductCommentsContract.View {

    private int productId;
    private RecyclerView rvComments;
    private TextView tvEmptyHint;
    private ProductCommentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_comments);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        productId = getIntent().getIntExtra("product_id", -1);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        TextView tvWriteReview = findViewById(R.id.tvWriteReview);
        tvWriteReview.setVisibility(View.VISIBLE);
        tvWriteReview.setOnClickListener(v -> presenter.writeReview());

        rvComments = findViewById(R.id.rvComments);
        tvEmptyHint = findViewById(R.id.tvEmptyHint);
        rvComments.setLayoutManager(new LinearLayoutManager(this));

        new ProductCommentsPresenter(this, this, productId).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.refresh();
        }
    }

    @Override
    public void showComments(List<ProductComment> comments, String currentUser) {
        if (comments.isEmpty()) {
            tvEmptyHint.setVisibility(View.VISIBLE);
            rvComments.setVisibility(View.GONE);
            return;
        }

        tvEmptyHint.setVisibility(View.GONE);
        rvComments.setVisibility(View.VISIBLE);
        adapter = new ProductCommentAdapter(comments, currentUser);
        adapter.setOnDeleteListener(this::showDeleteDialog);
        rvComments.setAdapter(adapter);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openAddComment(int productId) {
        Intent intent = new Intent(this, AddProductCommentActivity.class);
        intent.putExtra("product_id", productId);
        startActivity(intent);
    }

    private void showDeleteDialog(ProductComment comment) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        tvMessage.setText("确定删除这条评价吗？");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnDialogCancel).setOnClickListener(btn -> dialog.dismiss());

        android.widget.TextView btnConfirm = view.findViewById(R.id.btnDialogConfirm);
        btnConfirm.setBackgroundResource(R.drawable.bg_auth_button);
        btnConfirm.setOnClickListener(btn -> {
            dialog.dismiss();
            presenter.deleteComment(comment);
        });

        dialog.show();
    }
}
