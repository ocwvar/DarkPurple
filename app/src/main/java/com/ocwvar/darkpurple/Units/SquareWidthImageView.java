package com.ocwvar.darkpurple.Units;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Units
 * Data: 2016/8/7 15:08
 * Project: DarkPurple
 */
public class SquareWidthImageView extends ImageView {

    public SquareWidthImageView(Context context) {
        super(context);
    }

    public SquareWidthImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareWidthImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SquareWidthImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec,widthMeasureSpec);
    }


}
