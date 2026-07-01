package com.example.zhinongbao.view;

/* ============================================================
 * 【自定义控件 / 毛玻璃 / BlurBehindView】实时背景模糊控件（iOS 液态玻璃效果）
 * ============================================================
 *
 * 技术点：
 *   - 自定义 View（继承 View 自己画内容）。需要 Android 12 (API 31)+ 才支持 RenderEffect。
 *   - sourceView：要模糊的「背后内容」来源。
 *   - onDraw：每次重画时把 sourceView 当前样子抓进一张快照位图(snapshot)，再画到自己身上。
 *   - OnPreDrawListener：界面每帧绘制前触发，调用 invalidate() 让模糊持续刷新（实时）。
 *   - 用「半分辨率」位图做模糊（w/2、h/2）：省性能，模糊本来就看不清细节。
 *   - onAttached/onDetachedFromWindow：控件出现/移除时注册/注销监听并回收位图，防内存泄漏。
 *
 * 提示：在 IDE 里搜索「模糊」或「玻璃」可看用到它的地方（如 activity_main.xml 底部导航）。
 * ============================================================ */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.Nullable;

/**
 * 实时背景模糊 View（iOS 液态玻璃效果）。
 * 持续抓取 sourceView 在本视图位置下的内容，叠加 RenderEffect 高斯模糊。
 * 需要 API 31+。
 */
public class BlurBehindView extends View {

    private View sourceView;            // 要模糊的「背后内容」来源
    private Bitmap snapshot;            // 抓取背后内容用的快照位图
    private Canvas snapshotCanvas;      // 往快照位图上画内容的画布
    private float blurRadius = 25f;     // 模糊半径（越大越糊）
    private boolean drawingSnapshot = false;  // 防止「画快照」时又触发自己重画造成死循环

    // 每帧绘制前回调：只要不是正在画快照，就让自己重画一次，从而实现「实时」模糊
    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        if (!drawingSnapshot) invalidate();
        return true;
    };

    // 三个构造方法对应三种创建方式，统一交给 init()
    public BlurBehindView(Context c) { super(c); init(); }
    public BlurBehindView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public BlurBehindView(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(); }

    // 初始化：给自己设置高斯模糊渲染效果
    private void init() {
        setRenderEffect(RenderEffect.createBlurEffect(
                blurRadius, blurRadius, Shader.TileMode.CLAMP));
    }

    // 指定要模糊的背后内容来源
    public void setSourceView(View source) {
        this.sourceView = source;
    }

    // 动态调整模糊程度
    public void setBlurRadius(float radius) {
        this.blurRadius = radius;
        setRenderEffect(RenderEffect.createBlurEffect(
                radius, radius, Shader.TileMode.CLAMP));
    }

    // 控件被加到界面上时：注册「每帧绘制前」监听，开始持续刷新模糊
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    // 控件从界面移除时：注销监听并回收位图，释放内存
    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
        if (snapshot != null) { snapshot.recycle(); snapshot = null; }
        super.onDetachedFromWindow();
    }

    // 控件尺寸变化时：按「一半大小」重建快照位图（省性能）
    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w <= 0 || h <= 0) return;
        // 用半分辨率位图，模糊更便宜
        int bw = Math.max(1, w / 2);
        int bh = Math.max(1, h / 2);
        if (snapshot != null) snapshot.recycle();
        snapshot = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        snapshotCanvas = new Canvas(snapshot);
    }

    // 真正画内容：把背后 sourceView 当前的样子抓进快照，再放大画到自己身上（系统会自动加模糊）
    @Override
    protected void onDraw(Canvas canvas) {
        if (sourceView == null || snapshot == null || drawingSnapshot) return;

        drawingSnapshot = true;
        try {
            // 计算 sourceView 在屏幕上的位置 vs. 本 View 在屏幕上的位置
            int[] selfLoc = new int[2];
            int[] srcLoc = new int[2];
            getLocationInWindow(selfLoc);
            sourceView.getLocationInWindow(srcLoc);
            float dx = srcLoc[0] - selfLoc[0];
            float dy = srcLoc[1] - selfLoc[1];

            snapshot.eraseColor(0);
            snapshotCanvas.save();
            float scale = (float) snapshot.getWidth() / getWidth();
            snapshotCanvas.scale(scale, scale);
            snapshotCanvas.translate(dx, dy);
            sourceView.draw(snapshotCanvas);
            snapshotCanvas.restore();

            canvas.save();
            canvas.scale((float) getWidth() / snapshot.getWidth(),
                         (float) getHeight() / snapshot.getHeight());
            canvas.drawBitmap(snapshot, 0, 0, null);
            canvas.restore();
        } finally {
            drawingSnapshot = false;
        }
    }
}
