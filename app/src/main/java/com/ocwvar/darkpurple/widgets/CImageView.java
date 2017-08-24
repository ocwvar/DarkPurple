package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/9 14:49
 * File Location com.ocwvar.darkpurple.Units
 * 显示圆形图像的ImageView
 */

public final class CImageView extends AppCompatImageView {

    private float r, x, y;
    private int borderColor;

    /**
     * @param context     显示界面使用的Context
     * @param r           要绘制的半径
     * @param x           中心点X坐标
     * @param y           中心点Y坐标
     * @param borderColor 边界圆圈颜色
     */
    public CImageView(Context context, float r, float x, float y, int borderColor) {
        super(context);
        this.r = r;
        this.x = x;
        this.y = y;
        this.borderColor = borderColor;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension((int) (r * 2.0f), (int) (r * 2.0f));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable != null) {
            super.setImageDrawable(new CircleDrawable(((BitmapDrawable) drawable).getBitmap(), r, x, y, borderColor));
        } else {
            super.setImageDrawable(null);
        }
    }
}
