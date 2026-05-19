package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.ProductCommentAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.ProductComment;
import java.util.List;

public class ProductCommentsActivity extends AppCompatActivity {

    private int productId;
    private DataManager dm;
    private String currentUser;
    private RecyclerView rvComments;
    private TextView tvEmptyHint;
    private ProductCommentAdapter adapter;
    private List<ProductComment> comments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_comments);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        productId = getIntent().getIntExtra("product_id", -1);
        dm = DataManager.getInstance(this);
        currentUser = dm.getLoggedUser();

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        TextView tvWriteReview = findViewById(R.id.tvWriteReview);
        tvWriteReview.setVisibility(View.VISIBLE);
        tvWriteReview.setOnClickListener(v -> {
            if (dm.hasPurchasedProduct(currentUser, productId) || "admin".equals(currentUser)) {
                Intent intent = new Intent(this, AddProductCommentActivity.class);
                intent.putExtra("product_id", productId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "购买该商品后才可以进行评价哦", Toast.LENGTH_SHORT).show();
            }
        });

        rvComments = findViewById(R.id.rvComments);
        tvEmptyHint = findViewById(R.id.tvEmptyHint);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadComments();
    }

    private void loadComments() {
        comments = dm.getProductComments(productId);
        if (comments.isEmpty()) {
            tvEmptyHint.setVisibility(View.VISIBLE);
            rvComments.setVisibility(View.GONE);
        } else {
            tvEmptyHint.setVisibility(View.GONE);
            rvComments.setVisibility(View.VISIBLE);
            adapter = new ProductCommentAdapter(comments, currentUser);
            adapter.setOnDeleteListener(comment -> {
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
                    dm.deleteProductComment(comment.id);
                    loadComments();
                });

                dialog.show();
            });
            rvComments.setAdapter(adapter);
        }
    }
}
