package com.example.zhinongbao.view;

/* ============================================================
 * 【自定义控件 / 圆角容器 / RoundedClipFrame】可把内部子控件裁成圆角的布局
 * ============================================================
 *
 * 技术点：
 *   - 自定义 View：继承系统控件再扩展功能，本类继承 FrameLayout。
 *   - 三个构造方法：分别对应「代码 new / XML 里写 / 带样式」三种创建方式，
 *     都转去调用 init() 做统一初始化（这是自定义 View 的标准写法）。
 *   - ViewOutlineProvider + setClipToOutline(true)：给 View 设一个「轮廓」并按它裁剪。
 *   - cornerRadius：圆角大小，可在 XML 用自定义属性 app:cornerRadius 设置，也可代码设置。
 *
 * 提示：在 IDE 里搜索「圆角」或「RoundedClipFrame」可看哪里用到它。
 * ============================================================ */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import com.example.zhinongbao.R;

/** FrameLayout，按指定圆角半径裁剪所有子 View。 */
public class RoundedClipFrame extends FrameLayout {

    private float cornerRadius = 0f;   // 圆角半径（0 表示不裁圆角）

    // 三个构造方法对应三种创建方式，统一交给 init() 初始化
    public RoundedClipFrame(Context c) { super(c); init(c, null); }
    public RoundedClipFrame(Context c, @Nullable AttributeSet a) { super(c, a); init(c, a); }
    public RoundedClipFrame(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(c, a); }

    private void init(Context c, @Nullable AttributeSet a) {
        if (a != null) {
            // 从 XML 属性里读出 app:cornerRadius 的值
            TypedArray ta = c.obtainStyledAttributes(a, R.styleable.RoundedClipFrame);
            cornerRadius = ta.getDimension(R.styleable.RoundedClipFrame_cornerRadius, 0f);
            ta.recycle();   // 用完回收，释放资源
        }
        setClipToOutline(true);   // 开启「按轮廓裁剪」
        // 提供一个圆角矩形轮廓：系统据此把内容裁成圆角
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
    }

    // 代码动态设置圆角；invalidateOutline() 让轮廓立即重画生效
    public void setCornerRadius(float r) {
        this.cornerRadius = r;
        invalidateOutline();
    }
}
