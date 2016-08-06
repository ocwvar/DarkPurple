package com.ocwvar.darkpurple.Units.ImageLoader;

import android.graphics.Bitmap;

/**
 * Created by 区成伟
 * Date: 2016/2/18  20:25
 * 图片处理接口
 */

public interface OnHandleBitmap {

    /**
     * 图片异步处理回调
     * @param filePath  图片的路径
     * @param bitmap    图片对象
     * @return  交由剩余操作的图片对象
     */
    Bitmap onAsynHandleBitmap(String filePath, Bitmap bitmap);

    /**
     * 图片处理完成回调
     * @param bitmap    处理完成的图片对象
     */
    void onReduceCompleted(Bitmap bitmap);

}
