package com.example.zhinongbao.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zhinongbao.R;

/**
 * ============================================================
 * 【关于我们 / AboutUs】View（关于页 Activity）
 * 整体逻辑（关键步骤）：onCreate 加载 activity_about_us 布局，隐藏系统标题栏，
 *   绑定返回按钮关闭页面。内容为纯静态展示，无业务数据。
 * 数据来源：无（纯静态页面，不涉及 Repository 与数据库）。
 * 配合的文件：布局 = res/layout/activity_about_us.xml；由 SettingsActivity 跳转进入。
 * 在 MVP 数据流中的位置：View（界面层），无 Presenter（无业务逻辑）。
 * 提示：在 IDE 里搜索「关于我们」可看本组相关文件。
 * ============================================================
 */
public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
    }
}
