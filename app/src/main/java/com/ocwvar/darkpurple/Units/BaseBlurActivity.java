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
import android.support.v4.app.Fragment;
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
     * 切换Activity之前,调用这个方法进行模糊效果处理
     *
     * @param blurLevel     模糊程度 5 ~ 30
     * @param overDrawColor 模糊后遮罩颜色    Color.TRANSPARENT:不进行遮罩绘制   默认Color.argb(150,0,0,0)
     * @param colorless     去除模糊后图像的颜色
     * @param fromFragment  发起启动请求的Fragment
     * @param toActivity    要启动的Activity
     */
    public static void startBlurActivityForResultByFragment(int blurLevel, int overDrawColor, boolean colorless, final Fragment fromFragment, final Class<? extends Activity> toActivity, Bundle bundle, int requestCode) {

        if (blurLevel < 5) blurLevel = 5;
        else if (blurLevel > 30) blurLevel = 30;

        BG_BLUR_LEVEL = blurLevel;
        COLOR_OVERDRAW = overDrawColor;
        COLORLESS = colorless;

        //清空缓存的图像
        blurBGContainer.clear();
        blurBGContainer = new WeakReference<>(null);

        //获取发起请求的Activity的整个View
        View activityView = fromFragment.getActivity().getWindow().getDecorView();

        //开始绘制模糊背景
        setupBlur(activityView);

        //启动目标Activity
        Intent intent = new Intent(fromFragment.getActivity(), toActivity);
        if (bundle != null) intent.putExtras(bundle);
        fromFragment.startActivityForResult(intent, requestCode);
    }


    /**
     * 开始处理模糊效果
     *
     * @param sourceView 提供模糊图像的源View
     */
    @SuppressWarnings("deprecation")
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
            background = FastBlur.doBlur(background, BG_BLUR_LEVEL, true);

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= 21) {
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
