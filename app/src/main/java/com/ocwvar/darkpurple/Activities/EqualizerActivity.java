package com.ocwvar.darkpurple.Activities;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.ocwvar.darkpurple.Adapters.EqualizerSavedAdapter;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.BaseBlurActivity;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/12 16:41
 * File Location com.ocwvar.darkpurple.Activities
 * 均衡器设置界面
 */

public class EqualizerActivity extends BaseBlurActivity {

    final String TAG = "均衡器";

    EqualizerSavedAdapter adapter;
    RecyclerView recyclerView;

    @Override
    protected boolean onPreSetup() {

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(Color.argb(90, 0, 0, 0));
            getWindow().setStatusBarColor(Color.argb(0, 0, 0, 0));
        }

        return false;
    }

    @Override
    protected int setActivityView() {
        return R.layout.activity_equalizer;
    }

    @Override
    protected int onSetToolBar() {
        return 0;
    }

    @Override
    protected void onSetupViews(Bundle savedInstanceState) {

    }

    @Override
    protected void onViewClick(View clickedView) {

    }

    @Override
    protected boolean onViewLongClick(View holdedView) {
        return false;
    }


}
