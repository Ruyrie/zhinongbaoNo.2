package com.example.zhinongbao;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zhinongbao.adapter.UserAdapter;
import com.example.zhinongbao.data.DataManager;
import com.example.zhinongbao.model.User;
import java.util.List;

/** 账号管理界面：列出所有账号，支持短按修改密码、长按删除、添加账号 */
public class AccountManagerActivity extends AppCompatActivity {

    private DataManager dm;
    private List<User> users;
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_manager);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        dm = DataManager.getInstance(this);

        RecyclerView rv = findViewById(R.id.rvUsers);
        rv.setLayoutManager(new LinearLayoutManager(this));

        users = dm.getUsers();
        adapter = new UserAdapter(users, new UserAdapter.OnItemListener() {
            @Override
            public void onShortClick(User user) {
                // 修改密码
                Intent intent = new Intent(AccountManagerActivity.this, ChangePasswordActivity.class);
                intent.putExtra("username", user.username);
                startActivity(intent);
            }

            @Override
            public void onLongClick(User user) {
                // 长按删除，不能删除当前登录账号
                String current = dm.getLoggedUser();
                if (user.username.equals(current)) {
                    android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
                    android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
                    android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
                    tvTitle.setText("提示");
                    tvMessage.setText("不能删除当前登录的账号");

                    androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(
                            AccountManagerActivity.this)
                            .setView(view)
                            .create();
                    if (dialog.getWindow() != null)
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                    view.findViewById(R.id.btnDialogCancel).setVisibility(android.view.View.GONE);
                    android.widget.TextView btnConfirm = view.findViewById(R.id.btnDialogConfirm);
                    btnConfirm.setText("确定");
                    btnConfirm.setOnClickListener(btn -> dialog.dismiss());
                    dialog.show();
                    return;
                }

                android.view.View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
                android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
                android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
                tvTitle.setText("移除记录");
                tvMessage.setText("确定从本机登录历史中移除账号 \"" + user.username + "\" 吗？（账号本身不会被注销）");

                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(
                        AccountManagerActivity.this)
                        .setView(view)
                        .create();
                if (dialog.getWindow() != null)
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                android.widget.TextView btnConfirm = view.findViewById(R.id.btnDialogConfirm);
                btnConfirm.setText("移除");
                btnConfirm.setBackgroundResource(R.drawable.bg_auth_button);

                view.findViewById(R.id.btnDialogCancel).setOnClickListener(btn -> dialog.dismiss());
                btnConfirm.setOnClickListener(btn -> {
                    dialog.dismiss();
                    dm.hideUserFromHistory(user.username);
                    refreshList();
                });
                dialog.show();
            }
        });
        rv.setAdapter(adapter);

        // 添加账号按钮（跳转到注册页，添加模式）
        findViewById(R.id.btnAddAccount).setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra("add_mode", true);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        users.clear();
        users.addAll(dm.getUsers());
        adapter.notifyDataSetChanged();
    }
}
