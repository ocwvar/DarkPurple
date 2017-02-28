package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/9 14:49
 * File Location com.ocwvar.darkpurple.Units
 * 显示圆形图像的ImageView
 */

public class CImageView extends ImageView {

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

    public CImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
