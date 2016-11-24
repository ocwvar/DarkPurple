package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/13 15:49
 * File Location com.ocwvar.darkpurple.widgets
 * 绘制均衡器曲线
 */

public final class BezierView extends View {

    final int totalLevel = 20;
    final int totalEQCounts = 10;
    int[] currentLevels = new int[totalEQCounts];
    float[] pointYArray = new float[totalLevel + 1];
    float[] pointXArray = new float[totalEQCounts];

    Paint paint;
    Path path;

    public BezierView(Context context) {
        super(context);
        preSetup();
    }

    public BezierView(Context context, AttributeSet attrs) {
        super(context, attrs);
        preSetup();
    }

    public BezierView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        preSetup();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BezierView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        preSetup();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);

        final float viewHeight = getMeasuredHeight();
        final float viewWidth = getMeasuredWidth();

        final float eachHeight = viewHeight / totalLevel;
        final float eachWidth = viewWidth / totalEQCounts;

        for (int i = 0; i < totalLevel + 1; i++) {
            pointYArray[i] = getMeasuredHeight() - eachHeight * i;
        }

        for (int i = 0; i < totalEQCounts - 1; i++) {
            pointXArray[i] = eachWidth * i;
        }

        pointXArray[totalEQCounts - 1] = getMeasuredWidth();

    }

    /**
     * 预设置
     */
    private void preSetup() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3.0f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setDither(true);

        path = new Path();
    }

    /**
     * 设置均衡器等级
     *
     * @param eqIndex 均衡器位置
     * @param level   均衡器参数等级
     */
    public void setLevel(int eqIndex, int level) {
        currentLevels[eqIndex] = level;
        invalidate();
    }

    /**
     * 一次性设置所有均衡器等级
     *
     * @param levels 所有均衡器参数等级
     */
    public void setCurrentLevels(int[] levels) {
        for (int i = 0; i < levels.length; i++) {
            this.currentLevels[i] = levels[i] + 10;
        }
        invalidate();
    }

    /**
     * 重置均衡器曲线
     */
    public void resetLevel() {
        for (int i = 0; i < totalEQCounts; i++) {
            this.currentLevels[i] = 10;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //重置路径
        path.reset();

        //第一个起点
        int currentLevel = currentLevels[0];
        float nextStartPointYTemp = pointYArray[currentLevel];

        for (int i = 0; i < totalEQCounts - 1; i += 3) {

            final float startX = pointXArray[i];

            path.moveTo(startX, nextStartPointYTemp);

            final float pointX1 = pointXArray[i + 1];
            currentLevel = currentLevels[i + 1];
            final float pointY1 = pointYArray[currentLevel];

            final float pointX2 = pointXArray[i + 2];
            currentLevel = currentLevels[i + 2];
            final float pointY2 = pointYArray[currentLevel];

            final float pointX3 = pointXArray[i + 3];
            final float pointY3;

            if (i + 3 != 9) {
                pointY3 = ((pointYArray[currentLevels[i + 2]]) + (pointYArray[currentLevels[i + 3]]) + (pointYArray[currentLevels[i + 4]])) / 3;
            } else {
                currentLevel = currentLevels[i + 3];
                pointY3 = pointYArray[currentLevel];
            }

            nextStartPointYTemp = pointY3;

            path.cubicTo(pointX1, pointY1, pointX2, pointY2, pointX3, pointY3);
            canvas.drawPath(path, paint);
        }


    }

}
