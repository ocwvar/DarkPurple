package com.ocwvar.picturePicker.Units;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Project AnHe_Business
 * Created by 区成伟
 * On 2016/11/9 10:39
 * File Location com.ocwvar.anhe_business.Units
 * 等宽图片
 */

public class SameWidthImageView extends ImageView {

    public SameWidthImageView(Context context) {
        super(context);
    }

    public SameWidthImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SameWidthImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SameWidthImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, widthMeasureSpec);
    }
}
