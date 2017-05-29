package com.ocwvar.darkpurple;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.ocwvar.darkpurple.Adapters.AllMusicAdapter;
import com.ocwvar.darkpurple.Units.Logger;
import com.ocwvar.darkpurple.Units.MediaScanner;

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

    //下载歌曲文件储存目录
    public static final String DownloadMusicFolder = Environment.getExternalStorageDirectory().getPath() + "/DarkPurple/Restore/";

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

    //是否使用简易模式的播放界面
    public static boolean isUseSimplePlayingScreen = false;

    //是否自动跳转到播放界面
    public static boolean isAutoSwitchPlaying = true;

    //首页图像影响TabLayout颜色
    public static boolean isTabColorByImage = true;

    //频谱项 - 是否显示Node
    public static SpectrumStyles spectrumStyle = SpectrumStyles.Normal;

    //频谱项 - 是否显示Node
    public static boolean isSpectrumShowNode = true;

    //频谱项 - 是否显示外边线条
    public static boolean isSpectrumShowOutLine = true;

    //频谱项 - 是否显示柱状线
    public static boolean isSpectrumShowLine = true;

    //频谱项 - 频谱扩展值
    public static float spectrumRadio = 150.0f;

    //频谱项 - 频谱Node宽度
    public static float spectrumNodeWidth = 5.0f;

    //频谱项 - 频谱外边宽度
    public static float spectrumOutlineWidth = 8.0f;

    //频谱项 - 频谱柱状宽度
    public static float spectrumLineWidth = 8.0f;

    //频谱项 - 频谱数量
    public static int spectrumCounts = 80;

    //以下为储存界面尺寸数据  -1 为未初始化  0 为不存在数据
    //状态栏高度
    public static int StatusBarHeight = -1;

    //主界面列表样式
    public static AllMusicAdapter.LayoutStyle layoutStyle = AllMusicAdapter.LayoutStyle.Grid;

    //播放界面是否使用兼容模式
    public static boolean useCompatMode = false;

    /**
     * 初始化各项变量
     *
     * @param applicationContext 全局唯一 ApplicationContext 对象
     */
    public static void initDefaultValue(Context applicationContext) {
        if (applicationContext != null) {
            System.out.println();
            ApplicationContext = applicationContext;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

            //开始依次每条读取本地保存的数据

            UNKNOWN = applicationContext.getString(R.string.simple_unknown);

            MusicFolders = getPathSet();

            LengthLimited = Integer.parseInt(preferences.getString("length_limit", "35")) * 1000L;

            StatusBarHeight = preferences.getInt("StatusBarHeight", -1);

            IsFirstBoot = !preferences.contains("isNotFirstRunning");

            isListenMediaButton = preferences.getBoolean("isListenMediaButton", true);

            isResumeAudioWhenPlugin = preferences.getBoolean("isResumeAudioWhenPlugin", true);

            isUseSimplePlayingScreen = preferences.getBoolean("isUseSimplePlaying", false);

            isAutoSwitchPlaying = preferences.getBoolean("autoSwitchPlaying", true);

            isSpectrumShowNode = preferences.getBoolean("isSpectrumShowNode", true);

            isSpectrumShowOutLine = preferences.getBoolean("isSpectrumShowOutLine", true);

            isSpectrumShowLine = preferences.getBoolean("isSpectrumShowLine", true);

            isTabColorByImage = preferences.getBoolean("isTabColorByImage", true);

            spectrumNodeWidth = Float.valueOf(preferences.getString("spectrumNodeWidth", "5.0"));

            spectrumOutlineWidth = Float.valueOf(preferences.getString("spectrumOutlineWidth", "8.0"));

            spectrumLineWidth = Float.valueOf(preferences.getString("spectrum_line_width", "8.0"));

            spectrumCounts = Integer.valueOf(preferences.getString("spectrumCounts", "80"));

            spectrumRadio = Float.valueOf(preferences.getString("spectrumRadio", "150"));

            preferences.edit().putBoolean("isNotFirstRunning", true).apply();

            useCompatMode = preferences.getBoolean("useCompatMode", false);

            Color.loadColors();

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

            value = preferences.getString("MainList_Style", "0");
            switch (value) {
                case "0":
                    layoutStyle = AllMusicAdapter.LayoutStyle.Grid;
                    break;
                case "1":
                    layoutStyle = AllMusicAdapter.LayoutStyle.Linear;
                    break;
                default:
                    layoutStyle = AllMusicAdapter.LayoutStyle.Grid;
                    break;
            }

            value = preferences.getString("spectrum_style", "0");
            switch (value) {
                case "0":
                    spectrumStyle = SpectrumStyles.Normal;
                    break;
                case "1":
                    spectrumStyle = SpectrumStyles.OSU;
                    break;
                default:
                    spectrumStyle = SpectrumStyles.Circle;
                    break;
            }

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

            isUseSimplePlayingScreen = preferences.getBoolean("isUseSimplePlaying", false);

            isAutoSwitchPlaying = preferences.getBoolean("autoSwitchPlaying", true);

            isSpectrumShowNode = preferences.getBoolean("isSpectrumShowNode", true);

            isTabColorByImage = preferences.getBoolean("isTabColorByImage", true);

            isSpectrumShowOutLine = preferences.getBoolean("isSpectrumShowOutLine", true);

            isSpectrumShowLine = preferences.getBoolean("isSpectrumShowLine", true);

            spectrumNodeWidth = Float.valueOf(preferences.getString("spectrumNodeWidth", "5.0"));

            spectrumOutlineWidth = Float.valueOf(preferences.getString("spectrumOutlineWidth", "8.0"));

            spectrumLineWidth = Float.valueOf(preferences.getString("spectrum_line_width", "8.0"));

            spectrumCounts = Integer.valueOf(preferences.getString("spectrumCounts", "80"));

            spectrumRadio = Float.valueOf(preferences.getString("spectrumRadio", "150"));

            Color.loadColors();

            useCompatMode = preferences.getBoolean("useCompatMode", false);

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

            value = preferences.getString("MainList_Style", "0");
            switch (value) {
                case "0":
                    layoutStyle = AllMusicAdapter.LayoutStyle.Grid;
                    break;
                case "1":
                    layoutStyle = AllMusicAdapter.LayoutStyle.Linear;
                    break;
                default:
                    layoutStyle = AllMusicAdapter.LayoutStyle.Grid;
                    break;
            }

            value = preferences.getString("spectrum_style", "0");
            switch (value) {
                case "0":
                    spectrumStyle = SpectrumStyles.Normal;
                    break;
                case "1":
                    spectrumStyle = SpectrumStyles.OSU;
                    break;
                default:
                    spectrumStyle = SpectrumStyles.Circle;
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

    public enum SpectrumStyles {Normal, OSU, Circle}

    /**
     * APP 颜色资源
     */
    public static class Color {

        /**
         * 状态栏颜色
         */
        public static int StatusBar_color;

        /**
         * 导航栏颜色
         */
        public static int NavBar_Color;

        /**
         * ToolBar颜色
         */
        public static int ToolBar_color;

        /**
         * ToolBar副标题文字颜色
         */
        public static int ToolBar_subtitle_color;

        /**
         * ToolBar标题文字颜色
         */
        public static int ToolBar_title_color;

        /**
         * TabLayout颜色
         */
        public static int TabLayout_color;

        /**
         * TabLayout标题文字颜色  未选中状态
         */
        public static int TabLayout_title_color;

        /**
         * TabLayout标题文字颜色  选中状态
         */
        public static int TabLayout_title_color_selected;

        /**
         * TabLayout游标颜色
         */
        public static int TabLayout_Indicator_color;

        /**
         * 默认封面混合颜色
         */
        public static int DefaultCoverColor;

        /**
         * 频谱-外边颜色
         */
        public static int Spectrum_OutLine_Color;

        /**
         * 频谱-Node颜色
         */
        public static int Spectrum_Node_Color;

        /**
         * 频谱-柱状条颜色
         */
        public static int Spectrum_Line_Color;

        /**
         * 主界面背景颜色
         */
        public static int WindowBackground_Color;

        /**
         * 浮动按钮颜色
         */
        public static int FloatingButton_Color;

        /**
         * 列表状态 歌曲名称文字颜色
         */
        public static int Linear_Title_Color;

        /**
         * 列表状态 作曲家文字颜色
         */
        public static int Linear_Artist_Color;

        /**
         * 列表状态 时间长度文字颜色
         */
        public static int Linear_Time_Color;

        /**
         * 加载颜色资源
         * <p>
         * 优先从用户配置文件加载颜色资源 , 否则从资源文件加载
         */
        @SuppressWarnings("deprecation")
        static void loadColors() {

            final Context context = ApplicationContext;

            if (context == null) {
                //context为空 , 不能加载
                return;
            }

            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            if (Build.VERSION.SDK_INT >= 23) {
                StatusBar_color = sharedPreferences.getInt("StatusBar_Color", context.getColor(R.color.StatusBar_Color));
                NavBar_Color = sharedPreferences.getInt("NavBar_Color", context.getColor(R.color.NavBar_Color));
                ToolBar_color = sharedPreferences.getInt("ToolBar_color", context.getColor(R.color.ToolBar_color));
                ToolBar_title_color = sharedPreferences.getInt("ToolBar_title_color", context.getColor(R.color.ToolBar_title_color));
                ToolBar_subtitle_color = sharedPreferences.getInt("ToolBar_subtitle_color", context.getColor(R.color.ToolBar_subtitle_color));
                TabLayout_color = sharedPreferences.getInt("TabLayout_color", context.getColor(R.color.TabLayout_color));
                TabLayout_title_color = sharedPreferences.getInt("TabLayout_title_color", context.getColor(R.color.TabLayout_title_color));
                TabLayout_title_color_selected = sharedPreferences.getInt("TabLayout_title_color_selected", context.getColor(R.color.TabLayout_title_color_selected));
                TabLayout_Indicator_color = sharedPreferences.getInt("TabLayout_Indicator_color", context.getColor(R.color.TabLayout_Indicator_color));
                DefaultCoverColor = sharedPreferences.getInt("DefaultCoverColor", context.getColor(R.color.DefaultCoverColor));
                Spectrum_OutLine_Color = sharedPreferences.getInt("Spectrum_OutLine_Color", context.getColor(R.color.Spectrum_OutLine_Color));
                Spectrum_Node_Color = sharedPreferences.getInt("Spectrum_Node_Color", context.getColor(R.color.Spectrum_Node_Color));
                Spectrum_Line_Color = sharedPreferences.getInt("Spectrum_Line_Color", context.getColor(R.color.Spectrum_Line_Color));
                WindowBackground_Color = sharedPreferences.getInt("backgroundColor_Dark", context.getColor(R.color.backgroundColor_Dark));
                FloatingButton_Color = sharedPreferences.getInt("FloatingButton_Color", context.getColor(R.color.FloatingButton_Color));
                Linear_Title_Color = sharedPreferences.getInt("Linear_Title_Color", context.getColor(R.color.Linear_Title_Color));
                Linear_Artist_Color = sharedPreferences.getInt("Linear_Artist_Color", context.getColor(R.color.Linear_Artist_Color));
                Linear_Time_Color = sharedPreferences.getInt("Linear_Time_Color", context.getColor(R.color.Linear_Time_Color));
            } else {
                StatusBar_color = sharedPreferences.getInt("StatusBar_Color", context.getResources().getColor(R.color.StatusBar_Color));
                NavBar_Color = sharedPreferences.getInt("NavBar_Color", context.getResources().getColor(R.color.NavBar_Color));
                ToolBar_color = sharedPreferences.getInt("ToolBar_color", context.getResources().getColor(R.color.ToolBar_color));
                ToolBar_title_color = sharedPreferences.getInt("ToolBar_title_color", context.getResources().getColor(R.color.ToolBar_title_color));
                ToolBar_subtitle_color = sharedPreferences.getInt("ToolBar_subtitle_color", context.getResources().getColor(R.color.ToolBar_subtitle_color));
                TabLayout_color = sharedPreferences.getInt("TabLayout_color", context.getResources().getColor(R.color.TabLayout_color));
                TabLayout_title_color = sharedPreferences.getInt("TabLayout_title_color", context.getResources().getColor(R.color.TabLayout_title_color));
                TabLayout_title_color_selected = sharedPreferences.getInt("TabLayout_title_color_selected", context.getResources().getColor(R.color.TabLayout_title_color_selected));
                TabLayout_Indicator_color = sharedPreferences.getInt("TabLayout_Indicator_color", context.getResources().getColor(R.color.TabLayout_Indicator_color));
                DefaultCoverColor = sharedPreferences.getInt("DefaultCoverColor", context.getResources().getColor(R.color.DefaultCoverColor));
                Spectrum_OutLine_Color = sharedPreferences.getInt("Spectrum_OutLine_Color", context.getResources().getColor(R.color.Spectrum_OutLine_Color));
                Spectrum_Node_Color = sharedPreferences.getInt("Spectrum_Node_Color", context.getResources().getColor(R.color.Spectrum_Node_Color));
                Spectrum_Line_Color = sharedPreferences.getInt("Spectrum_Line_Color", context.getResources().getColor(R.color.Spectrum_Line_Color));
                WindowBackground_Color = sharedPreferences.getInt("backgroundColor_Dark", context.getResources().getColor(R.color.backgroundColor_Dark));
                FloatingButton_Color = sharedPreferences.getInt("FloatingButton_Color", context.getResources().getColor(R.color.FloatingButton_Color));
                Linear_Title_Color = sharedPreferences.getInt("Linear_Title_Color", context.getResources().getColor(R.color.Linear_Title_Color));
                Linear_Artist_Color = sharedPreferences.getInt("Linear_Artist_Color", context.getResources().getColor(R.color.Linear_Artist_Color));
                Linear_Time_Color = sharedPreferences.getInt("Linear_Time_Color", context.getResources().getColor(R.color.Linear_Time_Color));
            }

        }

        /**
         * 重置用户颜色配置文件
         */
        @SuppressLint("CommitPrefEdits")
        public static void resetColor() {

            final Context context = ApplicationContext;

            if (context == null) {
                return;
            }

            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

            editor.remove("StatusBar_Color");
            editor.remove("NavBar_Color");
            editor.remove("ToolBar_color");
            editor.remove("ToolBar_title_color");
            editor.remove("ToolBar_subtitle_color");
            editor.remove("TabLayout_color");
            editor.remove("TabLayout_title_color");
            editor.remove("TabLayout_title_color_selected");
            editor.remove("TabLayout_Indicator_color");
            editor.remove("DefaultCoverColor");
            editor.remove("Spectrum_Color");
            editor.remove("backgroundColor_Dark");
            editor.remove("FloatingButton_Color");
            editor.remove("Linear_Title_Color");
            editor.remove("Linear_Artist_Color");
            editor.remove("Linear_Time_Color");
            editor.remove("Spectrum_OutLine_Color");
            editor.remove("Spectrum_Node_Color");
            editor.remove("Spectrum_Line_Color");

            editor.commit();

            //在清空配置文件之后重新加载默认数据
            loadColors();
        }

    }

    /**
     * 用户数据资源
     */
    public static class USER {

        /**
         * 请求使用的TOKEN
         */
        public static String TOKEN;

        /**
         * 用户名
         */
        public static String USERNAME;

    }

}
