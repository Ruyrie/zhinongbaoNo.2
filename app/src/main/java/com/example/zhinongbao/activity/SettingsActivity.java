package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import com.example.zhinongbao.base.BaseMvpActivity;
import com.example.zhinongbao.mvp.settings.SettingsContract;
import com.example.zhinongbao.mvp.settings.SettingsPresenter;
import java.io.IOException;
import java.io.InputStream;

/**
 * ============================================================
 * 【设置 / Settings】View（设置页 Activity）
 * 整体逻辑（关键步骤）：
 *   1. onCreate 加载 activity_settings 布局，绑定返回按钮与各入口的跳转点击。
 *   2. new SettingsPresenter 注入业务逻辑。
 *   3. 点「退出登录」弹自定义确认对话框（dialog_confirm），确认后调 presenter.logout。
 *   4. Presenter 退出成功后回调 goLogin，清空任务栈跳回登录页。
 * 数据来源：退出登录经 SettingsPresenter 走 UserRepository；Repository 内部经
 *   ContentProvider 访问 SQLite，本类不直接碰数据库。
 * 配合的文件：接口约定 = mvp/settings/SettingsContract；业务逻辑 = mvp/settings/SettingsPresenter；
 *   布局 = res/layout/activity_settings.xml；跳转页面 = AccountManagerActivity /
 *   AddressManagerActivity / AddArticleActivity / AddProductActivity / AboutUsActivity / LoginActivity。
 * 在 MVP 数据流中的位置：View（界面层），通过 Presenter 处理退出，不直接访问数据库。
 * 提示：在 IDE 里搜索「设置」可看本组相关文件。
 * ============================================================
 */
public class SettingsActivity extends BaseMvpActivity<SettingsContract.Presenter> implements SettingsContract.View {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        loadAssetImage(findViewById(R.id.ivAddressManagerIcon), "dizhiguanli.png");

        new SettingsPresenter(this, this);

        findViewById(R.id.tvAccountManager)
                .setOnClickListener(v -> startActivity(new Intent(this, AccountManagerActivity.class)));

        findViewById(R.id.tvAddressManager)
                .setOnClickListener(v -> startActivity(new Intent(this, AddressManagerActivity.class)));

        findViewById(R.id.tvLogout).setOnClickListener(v -> confirmLogout());

        findViewById(R.id.tvAddArticle)
                .setOnClickListener(v -> startActivity(new Intent(this, AddArticleActivity.class)));

        findViewById(R.id.tvAddProduct)
                .setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        findViewById(R.id.tvAboutUs)
                .setOnClickListener(v -> startActivity(new Intent(this, AboutUsActivity.class)));
    }

    private void confirmLogout() {
        View view = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        android.widget.TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        android.widget.TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        tvTitle.setText("退出登录");
        tvMessage.setText("确定要退出登录吗？");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            presenter.logout();
        });
        dialog.show();
    }

    @Override
    public void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void loadAssetImage(ImageView iv, String filename) {
        if (iv == null) {
            return;
        }
        try (InputStream is = getAssets().open("pic/" + filename)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            iv.setImageBitmap(bmp);
        } catch (IOException ignored) {
        }
    }
}
