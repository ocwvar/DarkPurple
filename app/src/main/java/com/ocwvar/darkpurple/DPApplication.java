package com.ocwvar.darkpurple;

import android.app.Application;

import com.ocwvar.darkpurple.Units.ActivityManager;
import com.ocwvar.darkpurple.Units.EqualizerUnits;
import com.ocwvar.darkpurple.Units.PlaylistUnits;

/**
 * Created by 区成伟
 * Package: com.ocwvar.surfacetest.ExceptionHandler
 * Date: 2016/5/25  9:20
 * Project: SurfaceTest
 */
public class DPApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //CrashHandler.init(getApplicationContext());
        registerActivityLifecycleCallbacks(ActivityManager.getInstance());
        //Thread.setDefaultUncaughtExceptionHandler(new OCExceptionHandler(this));

        //初始化各项保存的设置
        AppConfigs.initDefaultValue(getApplicationContext());
        PlaylistUnits.getInstance().initSPData();
        EqualizerUnits.getInstance().init(getApplicationContext());

        if (AppConfigs.StatusBarHeight == -1) {
            //如果没有初始化 状态栏数据 , 则记录下来
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            int height = getResources().getDimensionPixelSize(resourceId);
            AppConfigs.updateLayoutData(AppConfigs.LayoutDataType.StatusBarHeight, height);
        }

    }


}
