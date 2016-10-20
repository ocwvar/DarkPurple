package com.ocwvar.darkpurple.Units;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/8/30 12:01
 * File Location com.ocwvar.darkpurple.Units
 * Activity界面管理
 */
public class ActivityManager {

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

    public void release() {
        if (activityList.size() > 0) {
            for (Activity activity : activityList) {
                activity.finish();
            }
            activityList.clear();
        }
    }

}
