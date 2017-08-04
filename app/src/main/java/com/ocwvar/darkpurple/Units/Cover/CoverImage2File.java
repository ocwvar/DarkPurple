package com.ocwvar.darkpurple.Units.Cover;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Units.Logger;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Units
 * Data: 2016/8/15 22:26
 * Project: DarkPurple
 * 封面图像缓存为本地文件工具
 */
public class CoverImage2File {

    private static final String TAG = "图像文件缓存";
    private static CoverImage2File imageCacher;

    public static CoverImage2File getInstance() {
        if (imageCacher == null) {
            imageCacher = new CoverImage2File();
        }
        return imageCacher;
    }

    /**
     * 生成唯一 TAG
     *
     * @param string 源数据
     * @return TAG字符串
     */
    public static String buildTag(String string) {
        string = string.replaceAll("[^\\w]", "");
        string = string.replaceAll(" ", "");
        return string;
    }

    /**
     * 将封面图像保存成本地文件
     *
     * @param coverType 图像类型
     * @param bitmap    图像Bitmap
     * @param coverID   音频的路径 , 用作唯一标识
     * @return 缓存得到的文件对象，缓存失败返回 NULL
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public
    @Nullable
    File makeImage2File(CoverType coverType, Bitmap bitmap, String coverID) {
        if (bitmap == null) {
            Logger.error(TAG, "图像文件无效");
            return null;
        } else if (!new File(AppConfigs.ImageCacheFolder).exists()) {
            //如果缓存文件夹不存在 , 则重新创建
            new File(AppConfigs.ImageCacheFolder).mkdirs();
        }

        //获取缓存图像文件对象
        final File imageFile = new File(getNormalCachePath(coverID, coverType));

        if (imageFile.exists() && imageFile.length() <= 0) {
            //图像文件虽然存在 , 但是图像文件无效 , 所以需要删除

            Logger.warning(TAG, "图像文件已损坏 , 已删除 , 进行重新缓存.");
            imageFile.delete();
        } else if (imageFile.exists() && imageFile.length() >= 1) {
            //图像文件已存在 , 并且有效 , 取消缓存

            Logger.warning(TAG, "图像已缓存 , 跳过操作.");
            return imageFile;
        }
        Logger.warning(TAG, "图像文件缓存中.");

        FileOutputStream fileOutputStream;

        try {
            if (!imageFile.exists()) {
                imageFile.createNewFile();
            }
            fileOutputStream = new FileOutputStream(imageFile, false);
        } catch (Exception e) {
            Logger.warning(TAG, "图像文件缓存失败.  开启文件输出流失败. 原因: " + e.getCause());
            return null;
        }

        boolean result = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);

        if (result) {
            //图像缓存成功
            Logger.warning(TAG, "图像缓存并更新缓存列表  成功");
            CoverManager.INSTANCE.setSource(coverType, coverID, imageFile.getPath(), true);
            return imageFile;
        } else {
            //图像缓存失败
            Logger.warning(TAG, "图像缓存  失败");
            return null;
        }
    }

    /**
     * 获取缓存图像相对路径
     *
     * @param audioPath 音频路径
     * @param coverType 封面类型
     * @return 缓存图像的相对路径
     */
    private String getNormalCachePath(String audioPath, CoverType coverType) {
        return AppConfigs.ImageCacheFolder + buildTag(audioPath) + "_" + coverType.name() + ".cache";
    }

}
