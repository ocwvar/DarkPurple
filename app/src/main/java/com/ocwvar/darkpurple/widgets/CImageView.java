package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/9 14:49
 * File Location com.ocwvar.darkpurple.Units
 */

public class CImageView extends ImageView {

    private float r, x, y;
    private int borderColor;

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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
