package com.ocwvar.darkpurple.Units;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.graphics.BitmapCompat;

import com.ocwvar.darkpurple.AppConfigs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Units
 * Data: 2016/8/15 22:26
 * Project: DarkPurple
 * 封面图像缓存为本地文件工具
 */
public class CoverImage2File {

    private static final String TAG = "CoverImage2File";
    public static CoverImage2File imageCacher;

    public static CoverImage2File getInstance() {
        if (imageCacher == null){
            imageCacher = new CoverImage2File();
        }
        return imageCacher;
    }

    /**
     * 将封面图像保存成本地文件
     * @param bitmap    图像Bitmap
     * @param audioPath 音频的路径 , 用作唯一标识
     * @return 缓存结果
     */
    public boolean makeImage2File(Bitmap bitmap , String audioPath){
        //获取缓存图像文件对象
        File imageFile = new File( getCachePath(audioPath) );

        if (imageFile.exists() && imageFile.length() <= 0){
            //图像文件虽然存在 , 但是图像文件无效 , 所以需要删除

            Logger.warnning(TAG,"图像文件已损坏 , 已删除 , 进行重新缓存.");
            imageFile.delete();
        }else if (imageFile.exists() && imageFile.length() >= 1){
            //图像文件已存在 , 并且有效 , 取消缓存

            Logger.warnning(TAG,"图像已缓存 , 跳过操作.");
            return true;
        }
        Logger.warnning(TAG,"图像文件缓存中.");

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(imageFile);
        } catch (FileNotFoundException e) {
            Logger.warnning(TAG,"图像文件缓存失败.  开启文件输出流失败.");
            imageFile = null;
            return false;
        }

        boolean result = bitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);

        if (result){
            //图像缓存成功
            Logger.warnning(TAG,"图像缓存  成功");
            return true;
        }else {
            //图像缓存失败
            Logger.warnning(TAG,"图像缓存  失败");
            return false;
        }
    }

    /**
     * 获取缓存图像路径
     * @param audioPath 音频路径
     * @return  图像路径
     */
    public String getCachePath(String audioPath){
        return AppConfigs.ImageCacheFolder+buildTag(audioPath)+".cache";
    }

    /**
     * 获取缓存图像文件对象
     * @param audioPath 音频路径
     * @return  文件对象
     */
    public File getCacheFile(String audioPath){
        return new File(AppConfigs.ImageCacheFolder+buildTag(audioPath)+".cache");
    }

    /**
     * 检测是否已经缓存了图像文件
     * @param audioPath 音频路径
     * @return  是否缓存
     */
    public boolean isAlreadyCached(String audioPath){
        File imageFile = new File(getCachePath(audioPath));
        if (imageFile.exists() && imageFile.length() <= 0){
            //图像文件存在.  但大小为0,文件无效
            imageFile.delete();
            imageFile = null;
            return false;
        }else if (imageFile.exists() && imageFile.length() >= 1){
            //图像文件存在. 可以使用
            imageFile = null;
            return true;
        }else{
            //图像文件不存在
            imageFile = null;
            return false;
        }
    }

    /**
     * 生成唯一 TAG
     * @param string    源数据
     * @return  TAG字符串
     */
    private String buildTag(String string){
        string = string.replaceAll("[^\\w]","");
        string = string.replaceAll(" ", "");
        return string;
    }

}
