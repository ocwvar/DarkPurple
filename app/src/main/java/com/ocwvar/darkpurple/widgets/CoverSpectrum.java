package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/10 3:22 PM
 * File Location com.ocwvar.darkpurple.widgets
 * This file use to :   自定义大小的SurfaceView，用于显示频谱动画
 */

public class CoverSpectrum extends SurfaceView {

    public CoverSpectrum(Context context) {
        super(context);
    }

    public CoverSpectrum(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(widthMeasureSpec));
    }
}
