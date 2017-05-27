package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/11/30 16:28
 * File Location com.ocwvar.darkpurple.widgets
 * 曲线滑动进度条
 */

public class LineSlider extends View implements View.OnTouchListener {

    //三个权重点 以及 一个移动控制点
    private CPoint startPoint, midPoint, endPoint, movingPoint;

    //线条和控制点的画笔对象
    private Paint linePaint, lightLinePaint, pointPaint;

    //滑动条路径和路径点存放列表容器
    private ArrayList<CPoint> pathPoints = null;
    private Path path;

    //整个View的宽度和高度
    private int viewWidth, viewHeight;

    //滑动条最大值
    private int VALUE_MAX = 100;

    //滑动条当前进度
    private int VALUE_PROGRESS = 0;

    //控制器半径大小
    private float sliderRadio = 25.0f;

    //进度条粗细大小
    private float lineWidth = 5.0f;

    //进度条颜色属性
    private int COLOR_LINE = Color.WHITE;

    //控制器当前进度颜色属性
    private int COLOR_SLIDER = Color.WHITE;

    //控制器剩余进度颜色属性
    private int COLOR_SLIDER_REST = Color.argb(50, 255, 255, 255);

    //标识 - 当前是否处于滑动控制状态
    private boolean isSliding = false;

    //接口
    private OnSlidingCallback callback;

    public LineSlider(Context context) {
        super(context);
    }

    public LineSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        initResource();
    }

    /**
     * 初始化必要的數據
     */
    private void initResource() {
        //创建画笔
        initPainter();

        //重置权重点
        resetPointsPosition(viewWidth, viewHeight);

        //生成路径
        path = new Path();
        pathPoints = new ArrayList<>();
        path.moveTo(startPoint.x, startPoint.y);
        path.quadTo(
                midPoint.x, midPoint.y,
                endPoint.x, endPoint.y
        );

        //创建路径储存容器列表
        pathPoints = new ArrayList<>();

        //计算路径点坐标
        calculatePathPoint(VALUE_MAX, path);

        //设置滑动监听
        setOnTouchListener(this);
    }

    /**
     * 设置滑动器颜色
     *
     * @param color 颜色值
     */
    public void setSliderColor(int color) {
        this.COLOR_SLIDER = color;
        invalidate();
    }

    /**
     * 设置线条颜色
     *
     * @param color 颜色值
     */
    public void setLineColor(int color) {
        this.COLOR_LINE = color;
        invalidate();
    }

    /**
     * 设置线条宽度   范围： 1～10
     *
     * @param lineWidth 线条宽度
     */
    public void setLineWidth(float lineWidth) {
        if (lineWidth > 0.0f && lineWidth <= 10.0f) {
            this.lineWidth = lineWidth;
            invalidate();
        }
    }

    /**
     * 设置滑动器半径  范围： 1.0～40.0
     *
     * @param sliderRadio 滑动器半径
     */
    public void setSliderRadio(float sliderRadio) {
        if (sliderRadio > 0.0f && sliderRadio <= 40.0f) {
            this.sliderRadio = sliderRadio;
            invalidate();
        }
    }

    /**
     * 获取当前进度
     */
    public int getProgress() {
        return VALUE_PROGRESS;
    }

    /**
     * 设置进度
     *
     * @param value 当前进度
     */
    public synchronized void setProgress(int value) {
        if (VALUE_MAX == value && value == 0) {
            return;
        }
        //先处理值的大小
        value = (value >= VALUE_MAX) ? VALUE_MAX : value;
        value = (value <= 0) ? 0 : value;

        VALUE_PROGRESS = value;
        invalidate();
    }

    /**
     * 获取本次进度的最大值
     */
    public int getMax() {
        return VALUE_MAX;
    }

    /**
     * 设置最大值
     *
     * @param value 最大值
     */
    public synchronized void setMax(int value) {
        this.VALUE_MAX = value;
        resetPointsPosition(viewWidth, viewHeight);
        calculatePathPoint(value, path);
        invalidate();
    }

    /**
     * 设置滑动监听回调
     *
     * @param callback 回调对象
     */
    public void setOnSlidingCallback(OnSlidingCallback callback) {
        this.callback = callback;
    }

    /**
     * 通过值来计算路径的分割点
     *
     * @param value 计算的值
     * @param path  路径对象
     */
    private void calculatePathPoint(int value, Path path) {
        if (value > 0 && path != null) {
            //清空上一次的路径值列表
            pathPoints.clear();

            //解析路径，获取Path总长度
            final PathMeasure pathMeasure = new PathMeasure(path, false);
            final float pathLength = pathMeasure.getLength();
            final float preValue = pathLength / value;

            //遍历计算每一个点的坐标
            for (float i = 0; i < pathLength; i += preValue) {
                final float[] pos = new float[2];
                pathMeasure.getPosTan(i, pos, null);
                pathPoints.add(new CPoint(pos[0], pos[1]));
            }
        }
    }

    /**
     * 重置权重点和操作点的坐标
     *
     * @param viewWidth  整个View的宽度
     * @param viewHeight 整个View的高度
     */
    private void resetPointsPosition(int viewWidth, int viewHeight) {
        startPoint = new CPoint(0, viewHeight * 0.5f);             //起始点
        midPoint = new CPoint(viewWidth * 0.5f, viewHeight);       //中间点
        endPoint = new CPoint(viewWidth, viewHeight * 0.5f);       //结尾点
        movingPoint = new CPoint(startPoint.x, startPoint.y);    //控制点
    }

    /**
     * 创建画笔对象
     */
    private void initPainter() {
        //线条画笔
        this.linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(COLOR_LINE);
        linePaint.setStrokeWidth(lineWidth);
        linePaint.setStyle(Paint.Style.STROKE);

        //剩余进度线条画笔
        this.lightLinePaint = new Paint();
        lightLinePaint.setAntiAlias(true);
        lightLinePaint.setColor(COLOR_SLIDER_REST);
        lightLinePaint.setStrokeWidth(lineWidth);
        lightLinePaint.setStyle(Paint.Style.STROKE);

        //控制器画笔
        this.pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setColor(COLOR_SLIDER);
        pointPaint.setStrokeWidth(1.0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pathPoints != null) {

            //绘制路径线
            float lastPointX = -1, lastPointY = -1;
            for (int i = 0; i < pathPoints.size(); i++) {
                final CPoint point = pathPoints.get(i);
                if (lastPointX >= 0 && lastPointY >= 0) {
                    if (i < VALUE_PROGRESS) {
                        canvas.drawLine(lastPointX, lastPointY, point.x, point.y, linePaint);
                    } else {
                        canvas.drawLine(lastPointX, lastPointY, point.x, point.y, lightLinePaint);
                    }
                }

                lastPointX = point.x;
                lastPointY = point.y;
            }

            //绘制结束点
            final CPoint finalPoint;
            if (VALUE_PROGRESS == pathPoints.size()) {
                finalPoint = pathPoints.get(VALUE_PROGRESS - 1);
            } else {
                finalPoint = pathPoints.get(VALUE_PROGRESS);
            }
            movingPoint.x = finalPoint.x;
            movingPoint.y = finalPoint.y;
            canvas.drawCircle(movingPoint.x, movingPoint.y, sliderRadio, pointPaint);

        }

    }

    /**
     * 当前是否在拖动器内
     *
     * @param touchX 触摸点X
     * @param touchY 触摸点Y
     * @return 是否在拖动器内部
     */
    private boolean isInsideSlider(float touchX, float touchY) {
        final int offset = 500;
        return (touchX >= movingPoint.x - sliderRadio - offset && touchX <= movingPoint.x + sliderRadio + offset)
                &&
                (touchY >= movingPoint.y - sliderRadio - offset && touchY <= movingPoint.y + sliderRadio + offset)
                ;
    }

    /**
     * 得到移动方向
     *
     * @param movingX 移动中的触摸点X轴
     * @return 移动方向
     */
    private synchronized SlidingWay getSlidingWay(float movingX) {
        if (movingX > movingPoint.x + 50) {
            return SlidingWay.Right;
        } else if (movingX < movingPoint.x - 50) {
            return SlidingWay.Left;
        } else {
            return null;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        final float touchX = motionEvent.getX();
        final float touchY = motionEvent.getY();

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //判断按下的坐标位置来确定是否触摸到控制点，如果是，则使“正在拖动”标识为 True
                isSliding = isInsideSlider(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isSliding) {
                    //只有处于拖动模式下，触摸移动才有效

                    final SlidingWay slidingWay = getSlidingWay(touchX);
                    if (slidingWay != null) {
                        switch (slidingWay) {
                            case Left:
                                setProgress(getProgress() - 1);
                                break;
                            case Right:
                                setProgress(getProgress() + 1);
                                break;
                        }

                        //反馈监听对象
                        if (callback != null) {
                            callback.onSliding(VALUE_PROGRESS, VALUE_MAX);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //当触摸点消失的时候，则结束拖动模式
                if (isSliding) {
                    //反馈监听对象
                    if (callback != null) {
                        callback.onStopSliding(VALUE_PROGRESS, VALUE_MAX);
                    }
                }

                isSliding = false;
                break;
        }
        invalidate();
        return true;
    }

    /**
     * 拖动方向
     */
    public enum SlidingWay {
        Right, Left
    }

    /**
     * 接口对象
     */
    public interface OnSlidingCallback {

        /**
         * 用户滑动中的回调
         *
         * @param progress 滑动时的进度
         * @param max      本次进度的最大值
         */
        void onSliding(int progress, int max);

        /**
         * 用户停止滑动的回调
         *
         * @param progress 停止时的进度
         * @param max      本次进度的最大值
         */
        void onStopSliding(int progress, int max);

    }

    /**
     * 点对象
     */
    final private class CPoint {

        float x;
        float y;

        CPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

}
