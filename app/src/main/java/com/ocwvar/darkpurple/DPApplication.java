package com.ocwvar.darkpurple;

import android.app.Application;

import com.ocwvar.darkpurple.Units.ActivityManager;
import com.ocwvar.darkpurple.Units.Cover.CoverManager;
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

        //加载所有保存的设置
        AppConfigs.initDefaultValue(getApplicationContext());

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
