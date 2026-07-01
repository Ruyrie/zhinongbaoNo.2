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

/**
 * ============================================================
 * 【商品评价列表 / Product Comments】View（界面/Activity）
 * 整体逻辑：onCreate 用 intent 里的 product_id 创建 Presenter 并 start()；
 *   Presenter 查好评价后回调 showComments 用 ProductCommentAdapter 渲染；
 *   点「写评价」调 presenter.writeReview()（会校验是否购买过）；删除时弹确认框，
 *   确认后调 presenter.deleteComment。onResume 时刷新，保证发完评价返回能看到。
 * 数据来源：不直接碰数据库；经 Presenter 走 repository/ProductRepository，
 *   Repository 内部通过 ContentProvider 访问 SQLite。
 * 配合的文件：接口约定 mvp/productcomments/ProductCommentsContract；业务逻辑
 *   ProductCommentsPresenter；列表适配器 adapter/ProductCommentAdapter；
 *   行布局 res/layout/item_product_comment.xml；页面布局
 *   res/layout/activity_product_comments.xml；确认弹窗 res/layout/dialog_confirm.xml；
 *   模型 model/ProductComment；会跳转到写评价页 AddProductCommentActivity。
 * 在 MVP 数据流中的位置：View 层。
 * 提示：在 IDE 里搜索「商品评价」可看本组相关文件。
 * ============================================================
 */
public class ProductCommentsActivity extends BaseMvpActivity<ProductCommentsContract.Presenter>
        implements ProductCommentsContract.View {

    private int productId;                     // 当前查看评价的商品 id
    private RecyclerView rvComments;           // 评价列表
    private TextView tvEmptyHint;              // 无评价时的空状态提示
    private ProductCommentAdapter adapter;     // 评价列表适配器

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
