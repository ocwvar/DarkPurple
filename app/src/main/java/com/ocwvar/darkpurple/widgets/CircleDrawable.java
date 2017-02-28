package com.ocwvar.darkpurple.widgets;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/9 14:40
 * File Location com.ocwvar.darkpurple.Units
 * 圆形Drawable
 */

class CircleDrawable extends Drawable {

    private Paint picPaint, circlePaint, borderPaint;
    private Bitmap bitmap;
    private Rect drawArea;
    private float r, x, y;

    /**
     * @param r           要绘制的半径
     * @param x           中心点X坐标
     * @param y           中心点Y坐标
     * @param borderColor 边界圆圈颜色
     */
    CircleDrawable(Bitmap bitmap, float r, float x, float y, int borderColor) {
        this.bitmap = bitmap;
        this.drawArea = new Rect();
        this.picPaint = new Paint();
        this.circlePaint = new Paint();
        this.borderPaint = new Paint();
        this.r = r;
        this.x = x;
        this.y = y;

        drawArea.set((int) (x - r), (int) (y - r), (int) (x + r), (int) (y + r));

        picPaint.setAntiAlias(true);
        circlePaint.setAntiAlias(true);
        borderPaint.setAntiAlias(true);

        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(4);
        borderPaint.setStyle(Paint.Style.STROKE);

        picPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int saveFlags = Canvas.MATRIX_SAVE_FLAG | Canvas.CLIP_SAVE_FLAG | Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.FULL_COLOR_LAYER_SAVE_FLAG | Canvas.CLIP_TO_LAYER_SAVE_FLAG;
        canvas.saveLayer(0, 0, x * 2, y * 2, null, saveFlags);
        canvas.drawCircle(x, y, r - 3, circlePaint);
        canvas.drawBitmap(bitmap, null, drawArea, picPaint);
        canvas.drawCircle(x, y, r - 3, borderPaint);
        canvas.restore();
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) x * 2;
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) y * 2;
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
