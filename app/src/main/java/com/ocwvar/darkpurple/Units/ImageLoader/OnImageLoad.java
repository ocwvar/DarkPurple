package com.ocwvar.darkpurple.Units.ImageLoader;

import android.graphics.Bitmap;

/**
 * Created by 区成伟
 * Date: 2016/2/6  14:12
 * 图片拉取状态接口
 */

public interface OnImageLoad {

    /**
     * 图片拉取完毕回调
     * @param image 得到的图片对象
     * @param tag   图片的唯一TAG
     */
    void onLoadCompleted(Bitmap image, String tag);

    /**
     * 图片拉取失败回调
     */
    void onLoadFailed();

}
