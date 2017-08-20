package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Project TestApplication
 * Created by OCWVAR
 * On 17-8-19 下午6:08
 * File Location com.ocwvar.testapplication
 * This file use to :   圆圈SeekBar
 */
public final class CircleSeekBar extends View {

    /**
     * 文字风格：不显示
     */
    public static final int TEXT_TYPE_HIDE = 0;
    /**
     * 文字风格：显示进度数值
     */
    public static final int TEXT_TYPE_PROGRESS = 1;
    /**
     * 文字风格：显示百分比
     */
    public static final int TEXT_TYPE_PERCENT = 2;
    //绘制圆形图像的区域对象
    private final RectF drawCircleArea = new RectF();
    //进度条画笔
    private final Paint circlePainter = new Paint();
    //文字画笔
    private final Paint textPainter = new Paint();
    //结束点画笔
    private final Paint pointPainter = new Paint();
    //半径
    private int R_LENGTH = 140;
    /**
     * 半径到边界的间隔
     * <p>
     * 1.当控件尺寸无法容纳 半径 + 间隔 时，将会优先考虑间隔，半径将会有所减少
     * 2.当间隔大于控件尺寸时，将会忽略间隔，则 R = 边长/2
     */
    private int PADDING = 30;
    //最大值
    private int VALUE_MAX = 100;
    //当前值
    private int VALUE_PROGRESS = 0;
    //每个值对应的角度
    private float VALUE_PRE_PROGRESS_ANGLE = 3.6f;
    //当前角度
    private double VALUE_ANGLE = 0.0f;
    //数据：长度
    private float NUMBER_WIDTH = 0.0f;
    //数据：一半长度
    private float NUMBER_HALF_WIDTH = 0.0f;
    //是否允许触摸控制进度
    private boolean enableTouch = true;
    //回调接口
    private Callback callback = null;
    //文字显示风格
    private int textType = TEXT_TYPE_PERCENT;

    public CircleSeekBar(Context context) {
        super(context);
        initPainters();
    }

    public CircleSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPainters();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();
        final Bundle bundle = new Bundle();
        bundle.putInt("textType", textType);
        bundle.putInt("VALUE_MAX", VALUE_MAX);
        bundle.putInt("VALUE_PROGRESS", VALUE_PROGRESS);
        bundle.putFloat("VALUE_PRE_PROGRESS_ANGLE", VALUE_PRE_PROGRESS_ANGLE);
        bundle.putDouble("VALUE_ANGLE", VALUE_ANGLE);
        bundle.putBoolean("enableTouch", enableTouch);
        bundle.putInt("R_LENGTH", R_LENGTH);
        bundle.putInt("PADDING", PADDING);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (state == null) {
            return;
        }
        final Bundle bundle = (Bundle) state;
        this.textType = bundle.getInt("textType");
        this.VALUE_MAX = bundle.getInt("VALUE_MAX");
        this.VALUE_PROGRESS = bundle.getInt("VALUE_PROGRESS");
        this.VALUE_PRE_PROGRESS_ANGLE = bundle.getFloat("VALUE_PRE_PROGRESS_ANGLE");
        this.VALUE_ANGLE = bundle.getDouble("VALUE_ANGLE");
        this.enableTouch = bundle.getBoolean("enableTouch");
        this.R_LENGTH = bundle.getInt("R_LENGTH");
        this.PADDING = bundle.getInt("PADDING");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //用最长的边作为边长，构成正方形
        final int length = (MeasureSpec.getSize(widthMeasureSpec) > MeasureSpec.getSize(heightMeasureSpec)) ? MeasureSpec.getSize(widthMeasureSpec) : MeasureSpec.getSize(heightMeasureSpec);

        //处理长度数据
        checkLengthData(length);

        //设置基础数据
        this.NUMBER_WIDTH = length;
        this.NUMBER_HALF_WIDTH = length >> 1;

        //设置绘制区域
        this.drawCircleArea.set(
                NUMBER_HALF_WIDTH - this.R_LENGTH,
                NUMBER_HALF_WIDTH - this.R_LENGTH,
                NUMBER_HALF_WIDTH + this.R_LENGTH,
                NUMBER_HALF_WIDTH + this.R_LENGTH
        );

        setMeasuredDimension(length, length);
    }

    /**
     * 设置相关的回调接口
     *
     * @param callback 回调接口对象，传入 NULL 进行取消
     */
    public void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
    }

    /**
     * 设置圆的半径
     *
     * @param R 半径值，如果无效则恢复默认值：控件尺寸的一半
     */
    public void setR(final int R) {
        if (R <= 0 || R > this.NUMBER_HALF_WIDTH - this.PADDING) {
            //如果设定的半径 小于等于0 、大于最大长度(控件一半长度 - 设定间隔)，则恢复最大长度
            this.R_LENGTH = (int) (this.NUMBER_HALF_WIDTH - this.PADDING);
        } else {
            this.R_LENGTH = R;
        }

        requestLayout();
        invalidate();
    }

    /**
     * 设置最大值，设定后将会导致进度恢复到 0
     *
     * @param max 最大值，不能小于 1
     */
    public void setMax(final int max) {
        if (max < 1) {
            return;
        } else {
            this.VALUE_MAX = max;
            this.VALUE_PROGRESS = 0;
            this.VALUE_PRE_PROGRESS_ANGLE = 360.0f / (float) this.VALUE_MAX;
        }

        invalidate();
    }

    /**
     * 设置当前进度
     *
     * @param progress 进度值
     */
    public void setProgress(final int progress) {
        if (progress <= 0) {
            this.VALUE_PROGRESS = 0;
            this.VALUE_ANGLE = 0d;

        } else if (progress >= this.VALUE_MAX) {
            this.VALUE_PROGRESS = this.VALUE_MAX;
            this.VALUE_ANGLE = 360d;

        } else {
            this.VALUE_PROGRESS = progress;
            this.VALUE_ANGLE = (progress * this.VALUE_PRE_PROGRESS_ANGLE >= 360f) ? 360d : progress * this.VALUE_PRE_PROGRESS_ANGLE;
        }

        invalidate();
        if (callback != null) {
            callback.onValueChanged(this.VALUE_MAX, this.VALUE_PROGRESS, false);
        }
    }

    /**
     * 设置当前进度
     *
     * @param angle 当前角度值
     */
    private void setProgress(final double angle) {
        if (angle <= 0d) {
            this.VALUE_ANGLE = 0.0d;
            this.VALUE_PROGRESS = 0;

        } else if (angle >= 360.0d) {
            this.VALUE_ANGLE = 360.0d;
            this.VALUE_PROGRESS = this.VALUE_MAX;

        } else {
            this.VALUE_ANGLE = angle;
            this.VALUE_PROGRESS = (int) (angle / this.VALUE_PRE_PROGRESS_ANGLE);
        }

        invalidate();
        if (callback != null) {
            callback.onValueChanged(this.VALUE_MAX, this.VALUE_PROGRESS, true);
        }
    }

    /**
     * @param enable 是否开启触摸控制进度，默认开启
     */
    public void setEnableTouch(final boolean enable) {
        this.enableTouch = enable;
        invalidate();
    }

    /**
     * @param textType 设置文字显示类型
     */
    public void setTextType(@IntRange(from = 0, to = 2) final int textType) {
        this.textType = textType;
        invalidate();
    }

    /**
     * @param color 圆圈的颜色
     */
    public void setCircleColor(@ColorInt final int color) {
        this.circlePainter.setColor(color);
        invalidate();
    }

    /**
     * @param width 圆圈厚度，默认：5.0f
     */
    public void setCircleWidth(final float width) {
        this.circlePainter.setStrokeWidth(width);
        invalidate();
    }

    /**
     * @param color 文字的颜色
     */
    public void setTextColor(@ColorInt final int color) {
        this.textPainter.setColor(color);
        invalidate();
    }

    /**
     * @param size 文字大小，默认：25.0f
     */
    public void setTextSize(final float size) {
        this.textPainter.setTextSize(size);
        invalidate();
    }

    /**
     * 检查并处理半径与间隔的数据，如果不正确则使之合适与控件的尺寸
     *
     * @param length 控件边长
     */
    private void checkLengthData(final int length) {
        //边长的一半
        final int halfOfLength = length / 2;

        if (halfOfLength < this.PADDING) {
            //如果间隔大于控件长度，则忽略
            this.PADDING = 0;

        } else if (halfOfLength < this.PADDING + this.R_LENGTH) {
            //控件尺寸小于间隔+半径，则缩小半径
            this.R_LENGTH = halfOfLength - this.PADDING;

        }
    }

    /**
     * 初始化画笔对象
     */
    private void initPainters() {
        this.circlePainter.setAntiAlias(true);
        this.circlePainter.setStrokeWidth(5.0f);
        this.circlePainter.setColor(Color.DKGRAY);
        this.circlePainter.setStyle(Paint.Style.STROKE);

        this.textPainter.setAntiAlias(true);
        this.textPainter.setTextSize(25.0f);
        this.textPainter.setTextAlign(Paint.Align.CENTER);
        this.textPainter.setColor(Color.DKGRAY);

        this.pointPainter.setAntiAlias(true);
        this.pointPainter.setColor(Color.DKGRAY);
        this.pointPainter.setStrokeWidth(5.0f);
    }

    /**
     * 获取触摸点的对应角度（中心点取控件中心点）
     *
     * @param x 触摸点 X
     * @param y 触摸点 Y
     * @return 对应的角度
     */
    private double getTouchAngle(final float x, final float y) {
        final float width = x - this.NUMBER_HALF_WIDTH;
        final float height = y - this.NUMBER_HALF_WIDTH;
        final float cosValue = height / (float) Math.sqrt(width * width + height * height);

        if (x < getHeight() >> 1) {
            return 180 + Math.acos(cosValue) * 57.2957795131f;
        } else {
            return 180 - Math.acos(cosValue) * 57.2957795131f;
        }
    }

    /**
     * 判断当前触摸区域是否是可以进行调节的区域
     *
     * @param x 触摸点 X
     * @param y 触摸点 Y
     * @return 是否可以进行调节
     */
    private boolean isInTouchArea(final float x, final float y) {
        final float width = this.NUMBER_HALF_WIDTH - x;
        final float height = this.NUMBER_HALF_WIDTH - y;
        final float length2Center = (float) Math.sqrt(width * width + height * height);
        return length2Center > (this.R_LENGTH - 50);    //50是指允许的误差范围
    }

    /**
     * @param r_scale 半径比例控制，传入 1.0f 则不变
     * @return 当前圆形最后一点的坐标[x, y]
     */
    private
    @NonNull
    float[] getCurrentCircleEdge(final float r_scale) {

        final float[] result = new float[]{0, 0};
        final float R = this.R_LENGTH * r_scale;

        if (R <= 0) {
            return result;
        } else {
            final double temp = (this.VALUE_ANGLE - 90.0d) * Math.PI / 180.0f;
            result[0] = this.NUMBER_HALF_WIDTH + R * (float) Math.cos(temp);
            result[1] = this.NUMBER_HALF_WIDTH + R * (float) Math.sin(temp);
            return result;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getAction();
        final float touchX = event.getX(0);
        final float touchY = event.getY(0);

        if (!enableTouch || !isInTouchArea(touchX, touchY)) {
            return false;
        }

        switch (action) {

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                setProgress(getTouchAngle(touchX, touchY));
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (callback != null) {
                    callback.onTouchFinish(this.VALUE_MAX, this.VALUE_PROGRESS);
                }
                break;
        }

        return true;
    }

    /**
     * 绘制文字
     *
     * @param canvas 画布
     */
    private void drawText(@Nullable final Canvas canvas) {
        if (this.textType == TEXT_TYPE_HIDE) {
            return;
        } else {
            final float drawX = getWidth() / 2;
            final float drawY = getHeight() / 2;

            switch (this.textType) {
                case TEXT_TYPE_PERCENT:
                    final float percent = ((float) this.VALUE_PROGRESS / (float) this.VALUE_MAX) * 100.0f;
                    canvas.drawText(String.format("%.2f％", percent), drawX, drawY, this.textPainter);
                    break;
                case TEXT_TYPE_PROGRESS:
                    canvas.drawText(Integer.toString(this.VALUE_PROGRESS), drawX, drawY, this.textPainter);
                    break;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);

        //尾点坐标
        final float[] edgePoint = getCurrentCircleEdge(1.0f);
        canvas.drawCircle(edgePoint[0], edgePoint[1], this.circlePainter.getStrokeWidth(), this.pointPainter);

        //绘制文字
        drawText(canvas);

        //绘制扇形图像
        canvas.drawArc(
                drawCircleArea,
                -90.0f,
                (float) this.VALUE_ANGLE,
                false,
                this.circlePainter
        );
    }

    interface Callback {

        /**
         * 进度发生变化回调接口
         *
         * @param max            最大值
         * @param progress       进度值
         * @param changedByTouch 是否由用户触摸发生变化
         */
        void onValueChanged(final int max, final int progress, final boolean changedByTouch);

        /**
         * 触摸调节结束时回调接口
         *
         * @param max      最大值
         * @param progress 进度值
         */
        void onTouchFinish(final int max, final int progress);

    }

}
