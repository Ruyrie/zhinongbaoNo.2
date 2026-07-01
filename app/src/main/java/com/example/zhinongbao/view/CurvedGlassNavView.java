package com.example.zhinongbao.view;

/* ============================================================
 * 【自定义控件 / 玻璃导航栏 / CurvedGlassNavView】带水滴凹槽的底部导航背景
 * ============================================================
 *
 * 技术点（图形绘制）：
 *   - Path（路径）：用一连串「直线 + 圆弧 + 贝塞尔曲线」描出导航栏外形轮廓。
 *   - 三次贝塞尔曲线 cubicTo：画出平滑的水滴凹槽（靠两个控制点弯出弧度）。
 *   - canvas.clipPath：把绘制范围裁剪成这个形状，子控件只在形状内显示。
 *   - setHolePosition(x)：外部（导航逻辑）调它来移动凹槽位置，触发 invalidate 重画。
 *   - holeX/holeWidth/holeDepth/cornerRadius：凹槽中心、宽、深和整体圆角参数。
 *
 * 提示：在 IDE 里搜索「导航」或「玻璃」可看它在 activity_main.xml 中的使用。
 * ============================================================ */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 带有滑动液态凹槽（水滴波纹）的自定义剪裁 FrameLayout。
 * 用于实现类似 iOS / 灵动岛 / 常见带水滴凹槽的底部导航背景。
 */
public class CurvedGlassNavView extends FrameLayout {

    private Path clipPath;        // 导航栏外形轮廓路径（含水滴凹槽）
    private Paint borderPaint;    // 画高光描边的画笔
    private Paint fillPaint;      // 画半透明白填充的画笔

    private float holeX = -1000f; // 凹槽中心 X 坐标（初始放屏幕外=不显示凹槽）
    private float holeWidth = 180f; // 凹槽总宽度
    private float holeDepth = 75f; // 凹槽深度
    private float cornerRadius = 90f; // 整体圆角

    // 两个构造方法对应「代码 new / XML 里写」两种创建方式，统一交给 init()
    public CurvedGlassNavView(@NonNull Context context) {
        super(context);
        init();
    }

    public CurvedGlassNavView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);   // 默认 ViewGroup 不自己画，这里打开，允许 draw() 画凹槽背景
        clipPath = new Path();

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setColor(Color.parseColor("#40FFFFFF")); // 玻璃发光高光描边

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#66FFFFFF")); // 半透明白色填充
    }

    /** 设置凹槽的中心位置，并触发重绘 */
    public void setHolePosition(float x) {
        this.holeX = x;
        invalidate();
    }

    // 尺寸变化时重新计算路径
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePath(w, h);
    }

    // 重新描出导航栏轮廓：四个圆角 + 顶部直线 + 中间水滴凹槽
    private void updatePath(int w, int h) {
        clipPath.reset();

        if (w == 0 || h == 0)
            return;

        float cr = cornerRadius;
        // 如果凹槽跑到屏幕外或者未初始化，不画凹槽
        float startHoleX = holeX - holeWidth / 2f;
        float endHoleX = holeX + holeWidth / 2f;

        clipPath.moveTo(0, cr);

        // 左上角
        clipPath.arcTo(new RectF(0, 0, cr * 2, cr * 2), 180, 90);

        // 顶部直线 + 凹槽
        if (holeX > 0 && holeX < w) {
            clipPath.lineTo(startHoleX, 0);

            // 使用三次贝塞尔曲线画出平滑的凹槽
            // 控制点 1 位于凹槽起始处的下方
            // 控制点 2 位于凹槽中心的左侧
            float cp1x = startHoleX + holeWidth * 0.25f;
            float cp1y = 0;
            float cp2x = holeX - holeWidth * 0.25f;
            float cp2y = holeDepth;
            clipPath.cubicTo(cp1x, cp1y, cp2x, cp2y, holeX, holeDepth);

            // 从凹槽中心回升
            float cp3x = holeX + holeWidth * 0.25f;
            float cp3y = holeDepth;
            float cp4x = endHoleX - holeWidth * 0.25f;
            float cp4y = 0;
            clipPath.cubicTo(cp3x, cp3y, cp4x, cp4y, endHoleX, 0);
        }

        clipPath.lineTo(w - cr, 0);

        // 右上角
        clipPath.arcTo(new RectF(w - cr * 2, 0, w, cr * 2), 270, 90);

        // 右边直线
        clipPath.lineTo(w, h - cr);

        // 右下角
        clipPath.arcTo(new RectF(w - cr * 2, h - cr * 2, w, h), 0, 90);

        // 底部直线
        clipPath.lineTo(cr, h);

        // 左下角
        clipPath.arcTo(new RectF(0, h - cr * 2, cr * 2, h), 90, 90);

        // 闭合
        clipPath.close();
    }

    // 真正绘制：先裁成凹槽形状画子控件，再叠加玻璃填充和高光边框
    @Override
    public void draw(Canvas canvas) {
        updatePath(getWidth(), getHeight());

        // 保存画布状态并裁剪为液态凹槽形状
        int saveCount = canvas.save();
        canvas.clipPath(clipPath);

        // 先让子 View (BlurBehindView 等) 绘制
        super.draw(canvas);

        // 在子 View 之上绘制半透明填充和高光边框，强化玻璃质感
        canvas.drawPath(clipPath, fillPaint);
        canvas.restoreToCount(saveCount);

        // 画边框不需要裁剪
        canvas.drawPath(clipPath, borderPaint);
    }
}
