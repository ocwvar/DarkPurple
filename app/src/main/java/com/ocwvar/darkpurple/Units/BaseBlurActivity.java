package com.ocwvar.darkpurple.Units;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * <p>模糊背景 Activity基类
 * <p>Project BlurBGActivityTest
 * <p>Created by 区成伟
 * <p>On 2016/9/21 13:58
 * <p>File Location com.ocwvar.blurbgactivitytest
 */

public abstract class BaseBlurActivity extends BaseActivity {

    //图像缩放倍数
    private final static int BG_BLUR_SCALE = 30;
    //背景默认颜色,显示于模糊处理失败或等待模糊处理过程中的Activity背景
    private final static int BG_DEFAULT_COLOR = Color.argb(255, 36, 44, 53);
    private static final Object locker = new Object();
    //图像模糊程度
    private static int BG_BLUR_LEVEL = 5;
    //绘制的遮罩颜色, Color.TRANSPARENT 则代表不绘制 , 默认 Color.argb(150,0,0,0)
    private static int COLOR_OVERDRAW = Color.argb(150, 0, 0, 0);
    //是否不保留模糊后的颜色
    private static boolean COLORLESS = false;
    private static WeakReference<Drawable> blurBGContainer = new WeakReference<>(null);

    /**
     * 切换Activity之前,调用这个方法进行模糊效果处理
     *
     * @param blurLevel     模糊程度 5 ~ 30
     * @param overDrawColor 模糊后遮罩颜色    Color.TRANSPARENT:不进行遮罩绘制   默认Color.argb(150,0,0,0)
     * @param colorless     去除模糊后图像的颜色
     * @param fromActivity  发起启动请求的Activity
     * @param toActivity    要启动的Activity
     */
    public static void startBlurActivity(int blurLevel, int overDrawColor, boolean colorless, final Activity fromActivity, final Class<? extends Activity> toActivity, Bundle bundle) {

        if (blurLevel < 5) blurLevel = 5;
        else if (blurLevel > 30) blurLevel = 30;

        BG_BLUR_LEVEL = blurLevel;
        COLOR_OVERDRAW = overDrawColor;
        COLORLESS = colorless;

        //清空缓存的图像
        blurBGContainer.clear();
        //启动目标Activity
        Intent intent = new Intent(fromActivity, toActivity);
        if (bundle != null) intent.putExtras(bundle);
        fromActivity.startActivity(intent);
        //获取发起请求的Activity的整个View
        View activityView = fromActivity.getWindow().getDecorView();
        //开始绘制模糊背景
        setupBlur(activityView);
    }

    /**
     * 切换Activity之前,调用这个方法进行模糊效果处理
     *
     * @param blurLevel     模糊程度 5 ~ 30
     * @param overDrawColor 模糊后遮罩颜色    Color.TRANSPARENT:不进行遮罩绘制   默认Color.argb(150,0,0,0)
     * @param colorless     去除模糊后图像的颜色
     * @param fromActivity  发起启动请求的Activity
     * @param toActivity    要启动的Activity
     */
    public static void startBlurActivityForResult(int blurLevel, int overDrawColor, boolean colorless, final Activity fromActivity, final Class<? extends Activity> toActivity, Bundle bundle, int requestCode) {

        if (blurLevel < 5) blurLevel = 5;
        else if (blurLevel > 30) blurLevel = 30;

        BG_BLUR_LEVEL = blurLevel;
        COLOR_OVERDRAW = overDrawColor;
        COLORLESS = colorless;

        //清空缓存的图像
        blurBGContainer.clear();
        blurBGContainer = new WeakReference<>(null);

        //获取发起请求的Activity的整个View
        View activityView = fromActivity.getWindow().getDecorView();

        //开始绘制模糊背景
        setupBlur(activityView);

        //启动目标Activity
        Intent intent = new Intent(fromActivity, toActivity);
        if (bundle != null) intent.putExtras(bundle);
        fromActivity.startActivityForResult(intent, requestCode);
    }

    /**
     * 开始处理模糊效果
     *
     * @param sourceView 提供模糊图像的源View
     */
    private static void setupBlur(View sourceView) {

        sourceView.setDrawingCacheEnabled(true);

        //获取界面图像数据
        Bitmap background = sourceView.getDrawingCache();

        //使图像缩小到指定倍数,减少工作量
        Matrix matrix = new Matrix();
        matrix.postScale(1.0f / BG_BLUR_SCALE, 1.0f / BG_BLUR_SCALE);
        //生产缩小后的图像
        background = Bitmap.createBitmap(background, 0, 0, background.getWidth(), background.getHeight(), matrix, true);

        //销毁View的缓存图像数据
        sourceView.destroyDrawingCache();

        //模糊后的Drawable图像
        Drawable blurDrawable;
        try {
            //进行模糊图像处理
            background = doBlur(background, BG_BLUR_LEVEL, true);

            if (COLORLESS) {
                //开始绘制灰度效果
                Canvas canvas = new Canvas(background);
                Paint paint = new Paint();
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setSaturation(0);
                paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
                canvas.drawBitmap(background, 0, 0, paint);
                paint = null;
                canvas = null;
            }

            if (COLOR_OVERDRAW != Color.TRANSPARENT) {
                //开始绘制遮罩效果
                Canvas canvas = new Canvas(background);
                canvas.drawColor(COLOR_OVERDRAW);
                canvas.drawBitmap(background, 0, 0, null);
                canvas = null;
            }

            //设置数据
            blurDrawable = new BitmapDrawable(background);
        } catch (Exception e) {
            //出现异常的时候 , 使用默认颜色作为背景
            blurDrawable = new ColorDrawable(BG_DEFAULT_COLOR);
        }

        blurBGContainer = new WeakReference<>(blurDrawable);
    }

    private static Bitmap doBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {

        // Stack Blur v1.0 from
        // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
        //
        // Java Author: Mario Klingemann <mario at quasimondo.com>
        // http://incubator.quasimondo.com
        // created Feburary 29, 2004
        // Android port : Yahel Bouaziz <yahel at kayenko.com>
        // http://www.kayenko.com
        // ported april 5th, 2012

        // This is a compromise between Gaussian Blur and Box blur
        // It creates much better looking blurs than Box Blur, but is
        // 7x faster than my Gaussian Blur implementation.
        //
        // I called it Stack Blur because this describes best how this
        // filter works internally: it creates a kind of moving stack
        // of colors whilst scanning through the image. Thereby it
        // just has to add one new block of color to the right side
        // of the stack and remove the leftmost color. The remaining
        // colors on the topmost layer of the stack are either added on
        // or reduced by one, depending on if they are on the right or
        // on the left side of the stack.
        //
        // If you are using this algorithm in your code please add
        // the following line:
        //
        // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= 21){
            //设置窗体标题栏透明
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        onShowBlurBackGround();

        super.onCreate(savedInstanceState);
    }

    /**
     * 执行显示模糊效果
     */
    private void onShowBlurBackGround() {

        Drawable blurBackground = blurBGContainer.get();
        if (blurBackground != null) {
            getWindow().getDecorView().setBackground(blurBackground);
        } else {
            getWindow().getDecorView().setBackgroundColor(BG_DEFAULT_COLOR);
        }

    }

}
