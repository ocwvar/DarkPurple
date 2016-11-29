package com.ocwvar.darkpurple.Units;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.SurfaceHolder;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Services.AudioCore;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;

import java.util.ArrayList;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/9/1 10:59
 * File Location com.ocwvar.darkpurple.Units
 * 显示频谱的SurfaceView控制器
 */
public class SurfaceViewControler implements SurfaceHolder.Callback {

    private SPShowerThread updateThread;
    private SurfaceHolder surfaceHolder;
    private int sfWidth = 0, sfHeight = 0;
    private boolean isDrawing = false;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //当SurfaceView创建的时候，获取 SurfaceHolder 对象
        surfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //当SurfaceView更改的时候，获取 SurfaceHolder 对象  （比如：横竖屏切换时 和 第一次创建的时候）
        //同时更新当前SurfaceView的尺寸数据，并终止旧的动画线程，创建新的线程
        surfaceHolder = holder;
        sfWidth = width;
        sfHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //当surfaceView被销毁的时候，终止动画显示线程
        surfaceHolder = null;
    }

    public boolean isDrawing() {
        return isDrawing;
    }

    /**
     * 开始频谱动画播放
     */
    public void start() {
        if (updateThread != null) {
            updateThread.interrupt();
            updateThread = null;
        }
        isDrawing = true;
        updateThread = new SPShowerThread(surfaceHolder);
        updateThread.start();
    }

    /**
     * 停止频谱动画播放
     */
    public void stop() {
        if (updateThread != null) {
            updateThread.interrupt();
            updateThread = null;
        }
    }

    /**
     * 更新动画的任务类
     */
    private final class SPShowerThread extends Thread {

        private final AudioService service;                       //音频服务
        private final Paint outlinePainter, linePaint, nodePaint;                             //线条，柱条，node点 的画笔
        private final int spectrumCount;                //绘制的条目数量
        private SurfaceHolder surfaceHolder;           //绘制的SurfaceHolder
        private Rect drawArea;                                //总绘制区域  (自动计算)
        private int r;                                                //频谱圆圈半径   (自动计算)
        private int centerX = sfWidth / 2;                  //绘制区域中心点  X 轴坐标
        private int centerY = sfHeight / 2;                 //绘制区域中心点  Y 轴坐标

        /**
         * 在构造方法中进行画笔的初始化
         *
         * @param surfaceHolder 用以更新UI的SurfaceHolder
         */
        SPShowerThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
            this.service = ServiceHolder.getInstance().getService();

            spectrumCount = (AppConfigs.spectrumCounts < 4) ? 4 : AppConfigs.spectrumCounts;

            //Node画笔
            if (AppConfigs.isSpectrumShowNode) {
                nodePaint = new Paint();
                nodePaint.setColor(AppConfigs.Color.Spectrum_Node_Color);
            } else {
                nodePaint = null;
            }

            //外部线条画笔
            if (AppConfigs.isSpectrumShowOutLine) {
                outlinePainter = new Paint();
                outlinePainter.setAntiAlias(true);
                outlinePainter.setColor(AppConfigs.Color.Spectrum_OutLine_Color);
                outlinePainter.setStrokeWidth(AppConfigs.spectrumOutlineWidth);
            } else {
                outlinePainter = null;
            }

            //柱状画笔
            if (AppConfigs.isSpectrumShowLine) {
                linePaint = new Paint();
                linePaint.setAntiAlias(true);
                linePaint.setColor(AppConfigs.Color.Spectrum_Line_Color);
                linePaint.setStrokeWidth(AppConfigs.spectrumLineWidth);
            } else {
                linePaint = null;
            }

        }

        @Override
        public void run() {

            //当音频处于已经加载的状态才进行显示
            if (service.getAudioStatus() != AudioCore.AudioStatus.Empty) {

                //先自动计算好各类必须的数据
                computeResourceSize();

                //根据计算得出的 r 半径 与设置的角度等分，计算圆圈上所有点的坐标
                final ArrayList<Point> points = splitCircle(centerX, centerY, r, spectrumCount);

                //画面更新线程
                updateThread(surfaceHolder, points);

                isDrawing = false;
            }

        }

        /**
         * 绘制图形线程主方法
         *
         * @param surfaceHolder 要绘制图形的 SurfaceHolder
         * @param points        频谱圆圈切割后的点集合
         */
        private void updateThread(SurfaceHolder surfaceHolder, ArrayList<Point> points) {

            final int pointsCount = points.size();

            int singleAreaCount = -1, nextOffset = 0;

            if (AppConfigs.isUseSecondStyleSpectrum) {
                singleAreaCount = computeEachArea(points);
            }

            //开始循环绘制部分
            while (surfaceHolder != null && !isInterrupted()) {

                //获取频谱数据
                final float[] fftDatas = service.getSpectrum();
                //获取画布
                final Canvas canvas = surfaceHolder.lockCanvas(drawArea);

                if (fftDatas != null && canvas != null) {

                    //清屏
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    canvas.drawColor(Color.rgb(36, 44, 54));

                    float lastX = -1, lastY = -1;
                    float firstX = -1, firstY = -1;

                    for (int i = 0, j = 0; i < points.size(); i++, j++) {

                        final Point point;


                        float fftData;
                        if (!AppConfigs.isUseSecondStyleSpectrum) {
                            //如果当前使用第一种风格 , 则不进行旋转 , 否则进行旋转动画
                            point = points.get(i);
                        } else if (i + nextOffset < pointsCount) {
                            point = points.get(i + nextOffset);
                        } else {
                            point = points.get((i + nextOffset) - pointsCount);
                        }

                        try {
                            if (AppConfigs.isUseSecondStyleSpectrum) {
                                //第二种绘制风格
                                fftData = fftDatas[j];
                                if (j >= singleAreaCount) {
                                    j = 0;
                                }
                            } else {
                                fftData = fftDatas[i];
                            }
                        } catch (IndexOutOfBoundsException e) {
                            fftData = 0f;
                        }

                        final float expX = point.getExpansion_X(fftData, 250);
                        final float expY = point.getExpansion_Y(fftData, 250);

                        //绘制Node点
                        if (AppConfigs.isSpectrumShowNode) {
                            canvas.drawCircle(expX, expY, AppConfigs.spectrumNodeWidth, nodePaint);
                        }

                        //绘制柱状条
                        if (AppConfigs.isSpectrumShowLine) {
                            canvas.drawLine(point.x, point.y, expX, expY, linePaint);
                        }

                        //绘制外部线条
                        if (AppConfigs.isSpectrumShowOutLine) {
                            if (i == points.size() - 1) {
                                //最后一个点的位置
                                canvas.drawLine(lastX, lastY, expX, expY, outlinePainter);
                                canvas.drawLine(expX, expY, firstX, firstY, outlinePainter);
                            } else if (lastX >= 0 && lastY >= 0) {
                                //首个～末尾  中间的位置
                                canvas.drawLine(expX, expY, lastX, lastY, outlinePainter);
                            } else if (i == 0) {
                                //首个的位置
                                firstX = expX;
                                firstY = expY;
                            }
                            lastX = expX;
                            lastY = expY;
                        }



                    }

                    if (AppConfigs.isUseSecondStyleSpectrum) {
                        //下一个偏移量增一
                        if (nextOffset + 1 < pointsCount) {
                            //只在点的数量范围内转一圈偏移度
                            nextOffset += 1;
                        } else {
                            //超过了则从头开始偏移
                            nextOffset = 0;
                        }
                    }

                    //更新画布
                    surfaceHolder.unlockCanvasAndPost(canvas);

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    //如果频谱数据为 NULL ， 则休眠 1000 毫秒再请求
                    surfaceHolder.unlockCanvasAndPost(canvas);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }

        /**
         * 计算每个区域的柱状图数量
         * <p>
         * 仅用于 显示第二种频谱样式 的状态下
         *
         * @param points 频谱圆圈切割后的点集合
         */
        private int computeEachArea(ArrayList<Point> points) {

            //分割区域块数
            final int areaCount = 6;
            //点的数量
            final int pointsCount = points.size();

            return pointsCount / areaCount;

        }

        /**
         * 计算画布区域、频谱圆圈半径
         */
        private void computeResourceSize() {
            if (sfHeight > sfWidth) {
                //竖屏状态屏幕尺寸
                r = sfWidth / 3;
                drawArea = new Rect(0, sfHeight / 4, sfWidth, sfHeight - sfWidth / 4);
            } else if (sfHeight < sfWidth) {
                //横屏状态屏幕尺寸
                r = sfHeight / 3;
                drawArea = new Rect(sfWidth / 4, 0, sfWidth - sfWidth / 4, sfHeight);
            } else {
                //正方形屏幕尺寸
                r = sfHeight / 3;
                drawArea = new Rect(0, 0, sfWidth, sfHeight);
            }

        }

        /**
         * 频谱圆圈切割
         *
         * @param centerX       绘制画布的中心 X坐标
         * @param centerY       绘制画布的中心 Y坐标
         * @param r             频谱圆圈半径
         * @param spectrumCount 每一份频谱条所占的角度
         * @return 频谱圆圈切割后的点集合
         */
        private ArrayList<Point> splitCircle(int centerX, int centerY, int r, int spectrumCount) {
            final ArrayList<Point> points = new ArrayList<>();

            //最小份数为 2
            if (spectrumCount <= 1) {
                spectrumCount = 2;
            }

            //计算每一份点应占有多少角度
            final float perAngel = 360 / spectrumCount;
            //起始绘制角度位置 0°
            float nextStartAngel = 0;

            for (int i = 0; nextStartAngel < 360f; i++) {

                Point point = null;

                if (i == 0) {
                    //特殊点，是圆的正上方第一个点。 0°
                    point = new Point(centerX, centerY - r);

                    //设置这个点的所属区域。下同。  5~8 ：特殊点区域   1~4：各个象限区域
                    point.setArea(5);
                } else {

                    if (nextStartAngel == 90f) {
                        //特殊点，是圆的正右方的第一个点。 90°
                        point = new Point(centerX + r, centerY);
                        point.setArea(6);
                    } else if (nextStartAngel == 180f) {
                        //特殊点，是圆的正下方的第一个点。 180°
                        point = new Point(centerX, centerY + r);
                        point.setArea(7);
                    } else if (nextStartAngel == 270f) {
                        //特殊点，是圆的正左方的第一个点。 270°
                        point = new Point(centerX - r, centerY);
                        point.setArea(8);
                    } else {
                        if (nextStartAngel > 0 && nextStartAngel < 90) {
                            //第一象限
                            point = new Point((float) (centerX + r * Math.sin(Math.toRadians(nextStartAngel))), (float) (centerY - r * Math.cos(Math.toRadians(nextStartAngel))));
                            point.setArea(1);

                        } else if (nextStartAngel > 90 && nextStartAngel < 180) {
                            //第二象限
                            point = new Point((float) (centerX + r * Math.cos(Math.toRadians(nextStartAngel - 90))), (float) (centerY + r * Math.sin(Math.toRadians(nextStartAngel - 90))));
                            point.setArea(2);

                        } else if (nextStartAngel > 180 && nextStartAngel < 270) {
                            //第三象限
                            point = new Point((float) (centerX - r * Math.sin(Math.toRadians(nextStartAngel - 180))), (float) (centerY + r * Math.cos(Math.toRadians(nextStartAngel - 180))));
                            point.setArea(3);

                        } else if (nextStartAngel > 270 && nextStartAngel < 360) {
                            //第四象限
                            point = new Point((float) (centerX - r * Math.cos(Math.toRadians(nextStartAngel - 270))), (float) (centerY - r * Math.sin(Math.toRadians(nextStartAngel - 270))));
                            point.setArea(4);

                        }
                    }
                }
                if (point != null) {
                    //给点的信息进行补充
                    point.setR(r);
                    point.setCenterX(centerX);
                    point.setCenterY(centerY);
                    point.setAngel(nextStartAngel);
                    point.setAngelTemp();
                    points.add(point);
                }
                nextStartAngel += perAngel;

            }
            return points;
        }

    }

    /**
     * 自定义的点对象
     */
    final private class Point {

        float x;
        float y;
        float angelTemp;
        int area;
        float angel;
        int r;
        int centerX, centerY;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        float getX() {
            return x;
        }

        void setX(float x) {
            this.x = x;
        }

        float getY() {
            return y;
        }

        void setY(float y) {
            this.y = y;
        }

        int getArea() {
            return area;
        }

        void setArea(int area) {
            this.area = area;
        }

        float getAngel() {
            return angel;
        }

        void setAngel(float angel) {
            this.angel = angel;
        }

        int getCenterX() {
            return centerX;
        }

        void setCenterX(int centerX) {
            this.centerX = centerX;
        }

        int getCenterY() {
            return centerY;
        }

        void setCenterY(int centerY) {
            this.centerY = centerY;
        }

        int getR() {
            return r;
        }

        void setR(int r) {
            this.r = r;
        }

        void setAngelTemp() {
            switch (area) {
                case 1:
                    //第一象限
                    this.angelTemp = (float) Math.sin(Math.toRadians(angel));
                    break;
                case 2:
                    //第二象限
                    this.angelTemp = (float) Math.cos(Math.toRadians(angel - 90));
                    break;
                case 3:
                    //第三象限
                    this.angelTemp = (float) Math.sin(Math.toRadians(angel - 180));
                    break;
                case 4:
                    //第四象限
                    this.angelTemp = (float) Math.cos(Math.toRadians(angel - 270));
                    break;
            }
        }

        /**
         * 得到点对应延伸后的点的 X 轴坐标
         *
         * @param fft           延伸的变量
         * @param expansionRate 延伸变量的倍数
         * @return 延伸后的点的 X 轴坐标
         */
        float getExpansion_X(float fft, int expansionRate) {
            switch (area) {
                case 1:
                case 2:
                    //第一象限
                    //第二象限
                    return centerX + (r + (fft * expansionRate)) * angelTemp;
                case 3:
                case 4:
                    //第三象限
                    //第四象限
                    return centerX - (r + (fft * expansionRate)) * angelTemp;
                case 5:
                case 7:
                default:
                    return x;
                case 6:
                    return x + fft * expansionRate;
                case 8:
                    return x - fft * expansionRate;
            }
        }

        /**
         * 得到点对应延伸后的点的 Y 轴坐标
         *
         * @param fft           延伸的变量
         * @param expansionRate 延伸变量的倍数
         * @return 延伸后的点的 Y 轴坐标
         */
        float getExpansion_Y(float fft, int expansionRate) {
            switch (area) {
                case 1:
                    //第一象限
                    return (float) (centerY - (r + (fft * expansionRate)) * Math.cos(Math.toRadians(angel)));
                case 2:
                    //第二象限
                    return (float) (centerY + (r + (fft * expansionRate)) * Math.sin(Math.toRadians(angel - 90)));
                case 3:
                    //第三象限
                    return (float) (centerY + (r + (fft * expansionRate)) * Math.cos(Math.toRadians(angel - 180)));
                case 4:
                    //第四象限
                    return (float) (centerY - (r + (fft * expansionRate)) * Math.sin(Math.toRadians(angel - 270)));
                case 5:
                    return y - fft * expansionRate;
                case 6:
                case 8:
                default:
                    return y;
                case 7:
                    return y + fft * expansionRate;
            }
        }

    }

}
