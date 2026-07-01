package com.example.zhinongbao.activity;

import com.example.zhinongbao.R;
import android.os.Bundle;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.yalantis.ucrop.UCropActivity;

/**
 * ============================================================
 * 【图片裁剪适配页 / UCropCompat】View（第三方裁剪库兼容 Activity）
 * 整体逻辑（关键步骤）：onCreate 先调用父类完成裁剪界面初始化，再监听
 *   WindowInsets，把状态栏与导航栏高度设为内容区上下 padding。
 * 数据来源：无（仅 UI 适配，不涉及 Repository 与数据库）。裁剪结果由启动它的
 *   页面通过 ActivityResult 接收。
 * 配合的文件：由 ProfileEditActivity（及其它需要头像/图片裁剪的页面）启动；
 *   依赖第三方库 com.yalantis.ucrop。
 * 在 MVP 数据流中的位置：辅助 View，不参与 MVP 业务数据流。
 * 提示：在 IDE 里搜索「图片裁剪」可看本组相关文件。
 * ============================================================
 */
public class UCropCompatActivity extends UCropActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View content = getWindow().getDecorView().findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
