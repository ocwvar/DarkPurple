package com.ocwvar.darkpurple.Units;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.ocwvar.darkpurple.Activities.MainFrameworkActivity;
import com.ocwvar.darkpurple.AppConfigs;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/8/30 12:01
 * File Location com.ocwvar.darkpurple.Units
 * Activity界面管理
 */
public class ActivityManager implements Application.ActivityLifecycleCallbacks {

    private static ActivityManager manager;
    private List<Activity> activityList;

    private ActivityManager() {
        this.activityList = new ArrayList<>();
    }

    public static ActivityManager getInstance() {
        if (manager == null) {
            manager = new ActivityManager();
        }
        return manager;
    }

    public void add(Activity activity) {
        activityList.add(0, activity);
    }

    public void remove(Activity activity) {
        activityList.remove(activity);
    }

    public void remove(int position) {
        try {
            activityList.remove(position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restartMainActivity() {
        release();
        Intent intent = new Intent(AppConfigs.ApplicationContext, MainFrameworkActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        AppConfigs.ApplicationContext.startActivity(intent);
    }

    public void release() {
        if (activityList.size() > 0) {
            for (Activity activity : activityList) {
                activity.finish();
            }
            activityList.clear();
        }
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        Logger.warning(" Activity生命周期监听 ", "发生创建    " + activity.getClass().getSimpleName());
        add(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Logger.warning(" Activity生命周期监听 ", "发生销毁    " + activity.getClass().getSimpleName());
        remove(activity);
    }

}
