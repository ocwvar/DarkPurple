package com.ocwvar.darkpurple.Units;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Project BlurBGActivityTest
 * Created by 区成伟
 * On 2016/9/21 13:13
 * File Location com.ocwvar.blurbgactivitytest
 */

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (onPreSetup()) {
            setContentView(setActivityView());
            onSetupViews();
        } else {
            finish();
        }
    }

    /**
     * 预先设置操作
     * </p>
     * 执行顺序: 0
     *
     * @return 是否进行接下来的操作 , False则会结束页面
     */
    protected abstract boolean onPreSetup();

    /**
     * 设置Activity使用的资源
     * </p>
     * 执行顺序: 1
     *
     * @return 布局资源
     */
    protected abstract int setActivityView();

    /**
     * 初始化控件的操作
     * </p>
     * 执行顺序: 2
     */
    @SuppressWarnings("ConstantConditions")
    protected abstract void onSetupViews();

    /**
     * 控件的点击事件
     *
     * @param clickedView 被点击的控件
     */
    protected abstract void onViewClick(View clickedView);

    /**
     * 长按的点击事件
     *
     * @param holdedView 被点击的控件
     */
    protected abstract boolean onViewLongClick(View holdedView);

    @Override
    public void onClick(View v) {
        onViewClick(v);
    }

    @Override
    public boolean onLongClick(View v) {
        return onViewLongClick(v);
    }

}
