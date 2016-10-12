package com.ocwvar.darkpurple.Units;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * Project BlurBGActivityTest
 * Created by 区成伟
 * On 2016/9/21 13:13
 * File Location com.ocwvar.blurbgactivitytest
 */

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private WeakReference<AlertDialog> simpleDialogContainer = new WeakReference<>(null);

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

    /**
     * 显示一个简易的对话框
     *
     * @param title   对话框标题
     * @param message 对话框信息
     */
    public void showMessageDialog(@NonNull Context activityContext, @NonNull String title, @NonNull String message) {
        AlertDialog dialog = simpleDialogContainer.get();
        if (dialog == null || activityContext == dialog.getContext()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);
            dialog = builder.create();

            simpleDialogContainer.clear();
            simpleDialogContainer = new WeakReference<>(dialog);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        onViewClick(v);
    }

    @Override
    public boolean onLongClick(View v) {
        return onViewLongClick(v);
    }

}
