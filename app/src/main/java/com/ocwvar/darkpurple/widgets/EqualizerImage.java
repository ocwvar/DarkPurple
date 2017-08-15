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
 * This file use to :   均衡器设置View
 */
public class EqualizerImage extends View {

    //// Y轴刻度范围 ：：必须为大于 1 的奇数，因为必须包含 一个中间点 和 上下对称的刻度长度
    private final static short Y_RANGE = 31;
    //// Y轴刻度中间值
    private final static short Y_RANGE_MID = (Y_RANGE - 1) / 2;

    //是否需要重新初始化数据
    private boolean needReInit = false;
    //回调接口
    private Callback callback = null;

    //点的坐标数组，初始化为空
    private EQLevel[] points = new EQLevel[0];
    //高度坐标
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
    private short touchingPoint = -1;
    //每个控制点的最右范围
    private float[] touchingPointsArea = null;
    //临时储存数组，用于可以进行初始化时获取数据，使用后置为 NULL
    private short[] temp = null;

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
        this.textPaint.setColor(Color.WHITE);

        this.linePaint = new Paint();
        this.linePaint.setColor(Color.argb(100, 127, 204, 255));
        this.linePaint.setStrokeWidth(2.0f);
    }

    /**
     * 初始化画笔Shader
     */
    private void initPaintShade() {
        final LinearGradient linearGradient = new LinearGradient(0, 0, 0, getBottom(), Color.argb(255, 127, 204, 255), Color.argb(0, 0, 0, 0), Shader.TileMode.MIRROR);
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
     * 通知回调接口所有数据发生了改变
     */
    private void onAllLevelChanged() {
        if (callback != null) {
            callback.onLevelChangeFinished(getAllLevels());
        }
    }

    /**
     * @return 当前所有等级
     */
    public
    @NonNull
    short[] getAllLevels() {
        final short[] result = new short[points.length];
        for (int i = 0; i < points.length; i++) {
            result[i] = points[i].getEqualizerLevel();
        }
        return result;
    }

    /**
     * 初始化点坐标
     *
     * @param pointsCount 点的数量
     * @return 初始化是否成功。
     */
    public boolean initPoints(final short pointsCount) {
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

            points[i] = new EQLevel((short) 0, x);
        }

        return true;
    }

    /**
     * 初始化所有等级刻度
     *
     * @param levelValues 等级数组，每个元素范围：-1500 ～ +1500
     * @param updateCallback 是否更新回调接口
     */
    public void initLevelValues(final short[] levelValues, final boolean updateCallback) {
        if (levelValues == null) {
            return;
        }

        if (needReInit) {
            //当前不可以进行初始化等级，需要等待View创建完成才可以
            this.temp = levelValues;
            return;
        }

        final int initLevelsLength = levelValues.length;

        for (int i = 0; i < points.length; i++) {

            if (i >= initLevelsLength) {
                //如果初始化的等级长度不够，则剩下的默认为 0
                points[i].setEqualizerLevel((short) 0);
            } else {
                if (levelValues[i] < -Y_RANGE_MID * 100) {
                    //小于最小值，则重新设置为最小值
                    levelValues[i] = -Y_RANGE_MID * 100;
                } else if (levelValues[i] > Y_RANGE_MID * 100) {
                    //大于最大值，则重新设置为最大值
                    levelValues[i] = Y_RANGE_MID * 100;
                }
                points[i].setEqualizerLevel(levelValues[i]);
            }

        }

        invalidate();

        if (updateCallback) {
            onAllLevelChanged();
        }
    }

    /**
     * 控制等级
     *
     * @param index          控制的位置
     * @param level          等级 -1500 ～ 1500
     * @param updateCallback 是否通知更新回调接口
     */
    public void adjustLevel(final short index, final short level, final boolean updateCallback) {
        if (points.length <= 0 || index < 0 || index >= points.length) {
            //请求的位置不正确
            return;
        } else if (level > Y_RANGE_MID * 100 || level < -Y_RANGE_MID * 100) {
            //控制的等级不正确
            return;
        }
        final EQLevel eqLevel = points[index];
        eqLevel.setEqualizerLevel(level);

        invalidate();

        if (updateCallback && callback != null) {
            callback.onLevelChanged(index, eqLevel.getEqualizerLevel());
        }
    }

    /**
     * 控制等级
     *
     * @param index          控制的位置
     * @param eqIndex        索引 0 ~ Y_RANGE
     * @param updateCallback 是否通知更新回调接口
     */
    public void adjustIndex(final short index, final short eqIndex, final boolean updateCallback) {
        if (points.length <= 0 || index < 0 || index >= points.length) {
            //请求的位置不正确
            return;
        } else if (eqIndex >= Y_RANGE || eqIndex < 0) {
            //控制的索引不正确
            return;
        }
        final EQLevel eqLevel = points[index];
        eqLevel.setIndex(eqIndex);

        invalidate();

        if (updateCallback && callback != null) {
            callback.onLevelChanged(index, eqLevel.getEqualizerLevel());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);

        if (this.needReInit) {
            initPoints((short) points.length);
            initPaintShade();
            initLevelValues(temp, false);
            this.needReInit = false;
            temp = null;
        }

        //计算图像
        final Path image = calculationPath();
        if (image != null) {
            canvas.drawPath(image, imagePaint);
        }

        //绘制每一个点
        for (final EQLevel eqLevel : points) {
            //绘制文字
            canvas.drawText(Integer.toString(eqLevel.getEqualizerLevel()), eqLevel.getX() - 10, eqLevel.getY() + 10, textPaint);
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
                for (short i = 0; i < touchingPointsArea.length; i++) {
                    if (touchX < touchingPointsArea[i]) {
                        touchingPoint = i;
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (touchY > yRanges[Y_RANGE_MID + 1] && touchY < yRanges[Y_RANGE_MID - 1]) {
                    //等级 0 范围
                    adjustLevel(touchingPoint, (short) 0, true);
                } else if (touchY < yRanges[Y_RANGE_MID + 1]) {
                    //大于等级 0
                    for (short i = Y_RANGE - 1; i > Y_RANGE_MID; i--) {
                        if (touchY < yRanges[i]) {
                            adjustIndex(touchingPoint, i, true);
                            break;
                        }
                    }
                } else if (touchY > yRanges[Y_RANGE_MID - 1]) {
                    //小于等级 0
                    for (short i = 0; i < Y_RANGE_MID; i++) {
                        if (touchY > yRanges[i]) {
                            adjustIndex(touchingPoint, i, true);
                            break;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                onAllLevelChanged();
                touchingPoint = -1;
                break;
        }

        return true;
    }

    public interface Callback {

        /**
         * 等级发生变化回调接口
         *
         * @param index          调节位置
         * @param equalizerLevel 均衡器的等级数据
         */
        void onLevelChanged(final short index, final short equalizerLevel);

        /**
         * 调节结束(手动调用Level调节、停止触摸调节)后，所有的等级数据
         *
         * @param equalizerLevels 所有的等级数据
         */
        void onLevelChangeFinished(final short[] equalizerLevels);

    }

    private final class EQLevel {

        //EqualizerLevel均衡器等级 ： -1500 ~ 1500
        private short equalizerLevel = 0;
        //Index索引：0 ~ 31
        private short index = 0;
        //次点所在的X轴坐标
        private float x = 0.0f;

        //  均衡器等级与索引对应表
        //
        //  -1500 -1400 -1300 -1200 -1100 -1000 -900 -800 -700 -600 -500 -400 -300 -200 -100  0  100 200 300 400 500 600 700 800 900 1000 1100 1200 1300 1400 1500
        //   0     1     2     3     4     5     6    7    8    9    10   11   12   13   14   15  16  17  18  19  20  21  22  23  24  25   26   27   28   29   30

        /**
         * @param equalizerLevel 均衡器等级
         * @param x              所在的X轴坐标
         */
        private EQLevel(final short equalizerLevel, final float x) {
            setEqualizerLevel(equalizerLevel);
            this.x = x;
        }

        private float getX() {
            return this.x;
        }

        private float getY() {
            return yRanges[index];
        }

        private short getEqualizerLevel() {
            return equalizerLevel;
        }

        private void setEqualizerLevel(short equalizerLevel) {
            //设置均衡器等级，需要同时转换得到对应的索引
            this.equalizerLevel = equalizerLevel;

            if (equalizerLevel == 0) {
                this.index = Y_RANGE_MID;
            } else if (equalizerLevel > 0) {
                this.index = (short) (Y_RANGE_MID + equalizerLevel / 100);
            } else {
                this.index = (short) (Y_RANGE_MID - equalizerLevel / -100);
            }
        }

        private short getIndex() {
            return index;
        }

        private void setIndex(short index) {
            //设置索引位置，需要同时转换得到对应的均衡器等级
            this.index = index;

            if (index == 15) {
                this.equalizerLevel = 0;
            } else if (index > 15) {
                this.equalizerLevel = (short) ((index - Y_RANGE_MID) * 100);
            } else {
                this.equalizerLevel = (short) ((Y_RANGE_MID - index) * -100);
            }
        }
    }

}
