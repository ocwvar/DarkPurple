package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/10 11:33 AM
 * File Location com.ocwvar.darkpurple.widgets
 * This file use to :   自定义大小的ViewPager，用于显示封面轮播
 */

public class CoverShowerViewPager extends ViewPager {

    public CoverShowerViewPager(Context context) {
        super(context);
    }

    public CoverShowerViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(widthMeasureSpec));
    }

}
