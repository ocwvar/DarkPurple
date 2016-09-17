package com.ocwvar.darkpurple;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.ocwvar.darkpurple.Units.Logger;
import com.ocwvar.darkpurple.Units.MediaScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple
 * Data: 2016/7/9 10:33
 * Project: DarkPurple
 * 应用的全局设置
 */
public class AppConfigs {
    //封面下载目录
    public static final String DownloadCoversFolder = Environment.getExternalStorageDirectory().getPath() + "/DarkPurple/DownloadCovers/";
    //图片缓存目录
    public static final String ImageCacheFolder = Environment.getExternalStorageDirectory().getPath() + "/DarkPurple/ImageCache/";
    //系统保留播放列表名字 , 用于缓存上一次的搜索记录
    public final static String CACHE_NAME = ".cached";
    //通过封面轮播切换歌曲等待时间
    public static final long switchPending = 600;
    //Json数据储存位置
    public static String JsonFilePath = Environment.getExternalStorageDirectory().getPath() + "/DarkPurple/JSONData/";
    //未知文字占位资源
    public static String UNKNOWN = "未知";
    //应用是否为第一次启动
    public static boolean IsFirstBoot = true;
    //设置扫描的路径数组集合 如果存在数据不为NULL , 则会扫描指定目录  否则扫描数据库
    public static String[] MusicFolders = null;
    //歌曲封面默认混合颜色
    public static int DefaultPaletteColor = Color.argb(108, 146, 51, 180);
    //全局唯一 ApplicationContext 对象
    public static Context ApplicationContext = null;
    //歌曲长度限制 , 小于限制的不当作歌曲添加
    public static long LengthLimited = 30000;
    //歌曲列表排序类型
    public static MediaScanner.SortType SortType = MediaScanner.SortType.ByName;
    //一次性信息储存文件名称
    public static String SP_ONCE = "ONCE";

    //是否监听耳机的多媒体按钮
    public static boolean isListenMediaButton = true;

    //是否在耳机重新插入/连接之后继续播放
    public static boolean isResumeAudioWhenPlugin = true;

    //以下为储存界面尺寸数据  -1 为未初始化  0 为不存在数据
    //状态栏高度
    public static int StatusBarHeight = -1;
    //导航栏高度
    public static int NevBarHeight = -1;

    /**
     * 初始化各项变量
     *
     * @param applicationContext 全局唯一 ApplicationContext 对象
     */
    public static void initDefaultValue(Context applicationContext) {
        if (applicationContext != null) {
            ApplicationContext = applicationContext;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

            //开始依次每条读取本地保存的数据

            UNKNOWN = applicationContext.getString(R.string.simple_unknown);

            MusicFolders = getPathSet();

            LengthLimited = Integer.parseInt(preferences.getString("length_limit", "35")) * 1000L;

            StatusBarHeight = preferences.getInt("StatusBarHeight", -1);

            NevBarHeight = preferences.getInt("NevBarHeight", -1);

            IsFirstBoot = !preferences.contains("isNotFirstRunning");

            isListenMediaButton = preferences.getBoolean("isListenMediaButton", true);

            isResumeAudioWhenPlugin = preferences.getBoolean("isResumeAudioWhenPlugin", true);

            preferences.edit().putBoolean("isNotFirstRunning", true).apply();

            String value = preferences.getString("scanner_sort_type", "0");
            switch (value) {
                case "0":
                    SortType = MediaScanner.SortType.ByName;
                    break;
                case "1":
                    SortType = MediaScanner.SortType.ByDate;
                    break;
                default:
                    SortType = MediaScanner.SortType.ByName;
                    break;
            }

            new File(ImageCacheFolder).mkdirs();
            new File(JsonFilePath).mkdirs();
            new File(DownloadCoversFolder).mkdirs();

        } else {
            Logger.error("初始化全局变量", "ApplicationContext 为空,无法读取数据");
        }

    }

    /**
     * 重新初始化菜单操作性的值
     */
    public static void reInitOptionValues() {
        if (ApplicationContext != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationContext);

            LengthLimited = Integer.parseInt(preferences.getString("length_limit", "35")) * 1000L;

            isListenMediaButton = preferences.getBoolean("isListenMediaButton", true);

            isResumeAudioWhenPlugin = preferences.getBoolean("isResumeAudioWhenPlugin", true);

            String value = preferences.getString("scanner_sort_type", "0");
            switch (value) {
                case "0":
                    SortType = MediaScanner.SortType.ByName;
                    break;
                case "1":
                    SortType = MediaScanner.SortType.ByDate;
                    break;
                default:
                    SortType = MediaScanner.SortType.ByName;
                    break;
            }

        }
    }

    /**
     * 保存歌曲文件夹列表数据
     *
     * @param source 数据源
     */
    public static void updatePathSet(ArrayList<String> source) {
        if (ApplicationContext != null && source != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationContext);
            SharedPreferences.Editor editor = preferences.edit();
            if (source.size() == 0) {
                //如果传入的数据集合大小为 0 , 则清空数据
                editor.remove("MusicFolder");
                MusicFolders = null;
            } else {
                editor.putStringSet("MusicFolder", new LinkedHashSet<>(source));
                MusicFolders = source.toArray(new String[source.size()]);
            }
            editor.apply();
        }
    }

    /**
     * 储存界面尺寸数据 , 用于下一次界面快速启动
     *
     * @param dataType 数据类型
     * @param data     数据
     */
    public static void updateLayoutData(LayoutDataType dataType, int data) {
        if (ApplicationContext != null) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ApplicationContext).edit();
            switch (dataType) {
                case StatusBarHeight:
                    StatusBarHeight = data;
                    editor.putInt("StatusBarHeight", data);
                    break;
                case NevBarHeight:
                    NevBarHeight = data;
                    editor.putInt("NevBarHeight", data);
                    break;
            }
            editor.apply();
        }
    }

    /**
     * 得到已储存的音乐文件夹路径集合
     *
     * @return 已储存的音乐文件夹集合  如果没有数据 则返回 NULL
     */
    private static String[] getPathSet() {
        if (ApplicationContext != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ApplicationContext);
            Set<String> stringSet = preferences.getStringSet("MusicFolder", null);
            if (stringSet != null && stringSet.size() > 0) {
                return stringSet.toArray(new String[stringSet.size()]);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public enum LayoutDataType {StatusBarHeight, NevBarHeight}

}
