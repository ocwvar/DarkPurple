package com.ocwvar.darkpurple;

import android.app.Application;

import com.netease.nis.bugrpt.CrashHandler;
import com.ocwvar.darkpurple.Units.ActivityManager;
import com.ocwvar.darkpurple.Units.Cover.CoverManager;
import com.ocwvar.darkpurple.Units.EqualizerHandler;
import com.ocwvar.darkpurple.Units.PlaylistUnits;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple
 * Date: 2016/5/25  9:20
 * Project: SurfaceTest
 */
public final class DPApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //友盟SDK初始化
        MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(DPApplication.this, "59950e977666134d4f000236", "Github", MobclickAgent.EScenarioType.E_UM_NORMAL));
        MobclickAgent.setCatchUncaughtExceptions(false);
        MobclickAgent.setDebugMode(true);

        //网易云捕初始化
        CrashHandler.init(getApplicationContext());

        //注册所有Activity的生命周期监听
        registerActivityLifecycleCallbacks(ActivityManager.getInstance());

        //加载所有保存的设置
        AppConfigs.initDefaultValue(getApplicationContext());

        //加载所有均衡器配置
        EqualizerHandler.INSTANCE.loadSavedEqualizerArgs();

        //加载所有保存的播放列表
        PlaylistUnits.getInstance().init();

        //加载所有封面和颜色的数据
        CoverManager.INSTANCE.initData();

        //如果没有状态栏高度数据，则进行获取并保存
        if (AppConfigs.StatusBarHeight == -1) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            int height = getResources().getDimensionPixelSize(resourceId);
            AppConfigs.updateLayoutData(AppConfigs.LayoutDataType.StatusBarHeight, height);
        }

    }


}
