package com.example.zhinongbao.view;

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

    private float cornerRadius = 0f;

    public RoundedClipFrame(Context c) { super(c); init(c, null); }
    public RoundedClipFrame(Context c, @Nullable AttributeSet a) { super(c, a); init(c, a); }
    public RoundedClipFrame(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(c, a); }

    private void init(Context c, @Nullable AttributeSet a) {
        if (a != null) {
            TypedArray ta = c.obtainStyledAttributes(a, R.styleable.RoundedClipFrame);
            cornerRadius = ta.getDimension(R.styleable.RoundedClipFrame_cornerRadius, 0f);
            ta.recycle();
        }
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
    }

    public void setCornerRadius(float r) {
        this.cornerRadius = r;
        invalidateOutline();
    }
}
