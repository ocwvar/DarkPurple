package com.ocwvar.darkpurple.Units.ImageLoader;

import android.graphics.Bitmap;

/**
 * Created by 区成伟
 * Date: 2016/3/12  10:28
 * 图片拉取完毕之后处理的回调接口
 */

public interface HandleOnLoaded {

    /**
     * 图片处理回调
     * @param bitmap    得到的图片对象
     * @param tag   图片的唯一TAG
     * @return  交由剩余操作的图片对象
     */
    Bitmap reduce(Bitmap bitmap, String tag);

}
