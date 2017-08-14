package com.ocwvar.darkpurple.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Project TestApplication
 * Created by OCWVAR
 * On 17-8-12 下午6:08
 * File Location com.ocwvar.testapplication
 * This file use to :   频谱控制View
 */
public final class EqualizerImage extends View {

    //// Y轴刻度范围 ：：必须为大于 1 的奇数，因为必须包含 一个中间点 和 上下对称的刻度长度
    private final static int Y_RANGE = 31;
    //// Y轴刻度中间值
    private final static int Y_RANGE_MID = (Y_RANGE - 1) / 2;

    //是否需要重新初始化数据
    private boolean needReInit = false;
    //回调接口
    private Callback callback = null;

    //点的坐标数组，初始化为空
    private EQLevel[] points = new EQLevel[0];
    //高度坐标，默认范围是：-10 ~ 0 ~ 10 一共21个刻度
    private float[] yRanges = new float[Y_RANGE];

    //点 画笔
    private Paint pointPaint;
    //图像 画笔
    private Paint imagePaint;
    //文字 画笔
    private Paint textPaint;
    //刻度 画笔
    private Paint linePaint;

    //触摸事件正在控制的EQ点，默认是 -1
    private int touchingPoint = -1;
    //每个控制点的最右范围
    private float[] touchingPointsArea = null;

    public EqualizerImage(Context context) {
        super(context);
        initPaints();
    }

    public EqualizerImage(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);
        this.needReInit = true;
    }

    public void setCallback(@NonNull final Callback callback) {
        this.callback = callback;
    }

    /**
     * 初始化画笔对象
     */
    private void initPaints() {
        this.pointPaint = new Paint();
        this.pointPaint.setColor(Color.WHITE);
        this.pointPaint.setStrokeWidth(5.0f);
        this.pointPaint.setStyle(Paint.Style.FILL);

        this.imagePaint = new Paint();
        this.imagePaint.setAntiAlias(true);
        this.imagePaint.setStrokeWidth(5.0f);
        this.imagePaint.setStyle(Paint.Style.FILL);

        this.textPaint = new Paint();
        this.textPaint.setAntiAlias(true);
        this.textPaint.setTextSize(30.0f);
        this.textPaint.setColor(Color.DKGRAY);

        this.linePaint = new Paint();
        this.linePaint.setColor(Color.argb(100, 127, 204, 255));
        this.linePaint.setStrokeWidth(2.0f);
    }

    /**
     * 初始化画笔Shader
     */
    private void initPaintShade() {
        final LinearGradient linearGradient = new LinearGradient(0, 0, 0, getBottom(), Color.argb(255, 127, 204, 255), Color.argb(60, 41, 169, 255), Shader.TileMode.MIRROR);
        this.imagePaint.setShader(linearGradient);
    }

    /**
     * 通过现有的坐标数据计算路径图像
     *
     * @return 路径图像，计算失败，返回NULL
     */
    private
    @Nullable
    Path calculationPath() {
        if (needReInit || points.length == 0) {
            return null;
        }

        final Path path = new Path();
        final float bottom = getBottom();
        final float right = getRight();

        //第一个点是左边中点
        path.moveTo(0, yRanges[Y_RANGE_MID]);

        //连接每一个点
        for (final EQLevel eqLevel : points) {
            path.lineTo(eqLevel.getX(), eqLevel.getY());
        }

        //最右边中点
        path.lineTo(right, yRanges[Y_RANGE_MID]);

        //连接成一个完整图形
        path.lineTo(right, bottom);
        path.lineTo(0, bottom);

        //闭合路径
        path.close();

        return path;
    }

    /**
     * 初始化点坐标
     *
     * @param pointsCount 点的数量
     * @return 初始化是否成功。
     */
    public boolean initPoints(final int pointsCount) {
        if (pointsCount <= 0) {
            return false;
        }

        //控件的宽度
        final int viewWidth = getMeasuredWidth();
        //控件的高度
        final int viewHeight = getMeasuredHeight();

        if (!needReInit && (viewHeight <= 0 || viewWidth <= 0)) {
            //设置需要重新初始化数据
            this.needReInit = true;
            //创建初始化数组
            points = new EQLevel[pointsCount];
            return true;
        } else if (pointsCount > viewWidth || Y_RANGE <= 2) {
            return false;
        } else {
            //重置初始化请求变量
            this.needReInit = false;
        }

        ////// 1.根据范围数量，计算高度Y轴坐标
        final float perYPadding = viewHeight / Y_RANGE;
        for (int i = 0; i < Y_RANGE; i++) {
            float y = perYPadding * (Y_RANGE - (i + 1));
            yRanges[i] = y;
        }
        yRanges[Y_RANGE - 1] += 10.0f;

        ////// 2.计算点的默认坐标以及最右触摸范围
        touchingPointsArea = new float[pointsCount];
        points = new EQLevel[pointsCount];
        //每一个点的间隔
        final float perPointPadding = viewWidth / pointsCount;

        //计算每个点的 X轴，Y轴 为支持的Y轴范围的中间值
        for (int i = 0; i < points.length; i++) {

            //每个点的 X轴 坐标为 (每个X轴间隔长度 × 位置) - ((每个X轴间隔长度 × 位置) × 1/ ( 位置 × 2 ))
            float x = perPointPadding * (i + 1);
            touchingPointsArea[i] = x;
            x -= x * (1.0f / ((i + 1) * 2.0f));

            points[i] = new EQLevel(0, x);
        }

        return true;
    }

    /**
     * 初始化所有等级刻度
     *
     * @param levelValues 等级数组，每个元素范围：-(Y_RANGE-1 / 2) ～ +(Y_RANGE-1 / 2)
     */
    public void initLevelValues(final int[] levelValues) {
        final int initLevelsLength = levelValues.length;

        for (int i = 0; i < points.length; i++) {
            if (levelValues[i] < -((Y_RANGE - 1) / 2)) {
                //小于最小值，则重新设置为最小值
                levelValues[i] = -((Y_RANGE - 1) / 2);
            } else if (levelValues[i] > ((Y_RANGE - 1) / 2)) {
                //大于最大值，则重新设置为最大值
                levelValues[i] = ((Y_RANGE - 1) / 2);
            }

            if (i >= initLevelsLength) {
                //如果初始化的等级长度不够，则剩下的默认为 0
                points[i].level = 0;
            } else {
                points[i].level = levelValues[i];
            }

        }

        invalidate();

        if (callback != null) {
            final int[] _levels = new int[points.length];
            final int[] _levelValues = new int[points.length];
            for (int i = 0; i < points.length; i++) {
                _levels[i] = points[i].getLevel();
                _levelValues[i] = points[i].level;
            }

            callback.onLevelsReset(_levels, _levelValues);
        }
    }

    /**
     * 控制等级
     *
     * @param index 控制的位置
     * @param level 等级 -10 ～ 10
     */
    public void adjustLevel(final int index, final int level) {
        if (points.length <= 0 || index < 0 || index >= points.length) {
            //请求的位置不正确
            return;
        } else if (level > (Y_RANGE - 1) / 2 || level < -((Y_RANGE - 1) / 2)) {
            //控制的等级不正确
            return;
        }
        final EQLevel eqLevel = points[index];
        eqLevel.setLevel(level);

        invalidate();

        if (callback != null) {
            callback.onLevelChanged(index, eqLevel.getLevel(), eqLevel.level);
        }
    }

    /**
     * @return 可以控制的点数量
     */
    public int totalPoints() {
        return this.points.length;
    }

    /**
     * @param index 要获取的位置
     * @return 获取位置无效返回 -999，范围： 0 ～ Y_RANGE
     */
    public int getLevelValue(final int index) {
        if (points.length <= 0 || index < 0 || index >= points.length) {
            return -999;
        }
        return points[index].getLevelValue();
    }

    /**
     * @param index 要获取的位置
     * @return 获取位置无效返回 -999，范围： -(Y_RANGE-1 / 2) ～ +(Y_RANGE-1 / 2)
     */
    public int getLevel(final int index) {
        if (points.length <= 0 || index < 0 || index >= points.length) {
            return -999;
        }
        return points[index].getLevel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);

        if (this.needReInit) {
            initPoints(points.length);
            initPaintShade();
        }

        //计算图像
        final Path image = calculationPath();
        if (image != null) {
            canvas.drawPath(image, imagePaint);
        }

        //绘制每一个点
        for (final EQLevel eqLevel : points) {
            //绘制文字
            canvas.drawText(Integer.toString(eqLevel.getLevelValue()), eqLevel.getX() - 10, eqLevel.getY() + 10, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (touchingPointsArea == null) {
            //无可辨识的触摸区域
            return true;
        }

        final int action = event.getAction();
        final float touchX = event.getX(0);
        final float touchY = event.getY(0);

        switch (action) {
            //按下事件，在这里确定控制的EQ点
            case MotionEvent.ACTION_DOWN:
                for (int i = 0; i < touchingPointsArea.length; i++) {
                    if (touchX < touchingPointsArea[i]) {
                        touchingPoint = i;
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (touchY > yRanges[Y_RANGE_MID + 1] && touchY < yRanges[Y_RANGE_MID - 1]) {
                    //等级 0 范围
                    adjustLevel(touchingPoint, 0);
                } else if (touchY < yRanges[Y_RANGE_MID + 1]) {
                    //大于等级 0
                    for (int i = Y_RANGE - 1; i > Y_RANGE_MID; i--) {
                        if (touchY < yRanges[i]) {
                            adjustLevel(touchingPoint, i - Y_RANGE_MID);
                            break;
                        }
                    }
                } else if (touchY > yRanges[Y_RANGE_MID - 1]) {
                    //小于等级 0
                    for (int i = 0; i < Y_RANGE_MID; i++) {
                        if (touchY > yRanges[i]) {
                            adjustLevel(touchingPoint, i - Y_RANGE_MID);
                            break;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                touchingPoint = -1;
                break;
        }

        return true;
    }

    public interface Callback {

        /**
         * 等级发生变化回调接口
         *
         * @param index      调节位置
         * @param level      等级：        0 ～ Y_RANGE
         * @param levelValue 等级刻度值：  -(Y_RANGE-1 / 2) ～ +(Y_RANGE-1 / 2)
         */
        void onLevelChanged(final int index, final int level, final int levelValue);

        /**
         * 等级发生重置回调接口
         *
         * @param levels      重置后所有等级的数组：       0 ～ Y_RANGE
         * @param levelValues 重置后所有等级刻度值的数组：-(Y_RANGE-1 / 2) ～ +(Y_RANGE-1 / 2)
         */
        void onLevelsReset(final int[] levels, final int[] levelValues);

    }

    private final class EQLevel {

        //  getLevel值：     0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20
        //  Level变量：    -10 -9 -8 -7 -6 -5 -4 -3 -2 -1  0  1  2  3  4  5  6  7  8  9 10
        private int level = 0;
        private float x = 0.0f;

        private EQLevel(int level, float x) {
            this.level = level;
            this.x = x;
        }

        float getX() {
            return this.x;
        }

        float getY() {
            return yRanges[getLevel()];
        }

        int getLevel() {
            return this.level + ((Y_RANGE - 1) / 2);
        }

        void setLevel(int level) {
            if (level >= -((Y_RANGE - 1) / 2) && level <= (Y_RANGE - 1) / 2) {
                this.level = level;
            }
        }

        int getLevelValue() {
            return this.level;
        }

    }

}
