package com.ocwvar.darkpurple.Units;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;

import com.ocwvar.darkpurple.AppConfigs;

import java.lang.ref.WeakReference;

/**
 * Project BlurBGActivityTest
 * Created by 区成伟
 * On 2016/9/21 13:13
 * File Location com.ocwvar.blurbgactivitytest
 * 基础Activity
 */

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private Toolbar toolbar;
    private Snackbar holdingSnackBar = null;

    //消息对话框 - 对话框弱引用容器
    private WeakReference<ProgressDialog> waitingProgress = new WeakReference<>(null);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(AppConfigs.Color.StatusBar_color);
            getWindow().setNavigationBarColor(AppConfigs.Color.NavBar_Color);
        }

        super.onCreate(savedInstanceState);

        if (onPreSetup()) {
            setContentView(setActivityView());
            settingToolBar(onSetToolBar());
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
     * 设置Activity使用的资源
     * </p>
     * 执行顺序: 2
     *
     * @return ToolBar 资源ID
     */
    protected abstract int onSetToolBar();

    /**
     * 初始化控件的操作
     * </p>
     * 执行顺序: 3
     */
    @SuppressWarnings("ConstantConditions")
    protected abstract void onSetupViews();

    /**
     * 设置默认ToolBar属性
     */
    private void settingToolBar(int resID) {
        Toolbar toolbar = (Toolbar) findViewById(resID);
        if (toolbar != null) {
            this.toolbar = toolbar;
            setSupportActionBar(toolbar);
            toolbar.setBackgroundColor(AppConfigs.Color.ToolBar_color);
            toolbar.setTitleTextColor(AppConfigs.Color.ToolBar_title_color);
            toolbar.setSubtitleTextColor(AppConfigs.Color.ToolBar_subtitle_color);
        }
    }

    /**
     * 显示一个消息对话框
     *
     * @param message     显示的消息
     * @param canBeCancel 是否可以取消
     */
    public void showMessageDialog(Context context, String message, boolean canBeCancel) {
        if (TextUtils.isEmpty(message) || context == null) {
            return;
        }
        ProgressDialog waitingProgress = this.waitingProgress.get();
        if (waitingProgress == null) {
            waitingProgress = new ProgressDialog(context);
            waitingProgress.setCanceledOnTouchOutside(false);
            this.waitingProgress = new WeakReference<>(waitingProgress);
        }
        waitingProgress.setMessage(message);
        waitingProgress.setCancelable(canBeCancel);
        waitingProgress.show();
    }

    /**
     * 取消正在显示的消息对话框
     */
    public void dismissMessageDialog() {
        final ProgressDialog progressDialog = waitingProgress.get();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * 显示固定的SnackBar通知条
     *
     * @param message 要显示的文字 , 文字为空则不显示
     */
    public void showHoldingSnackBar(@NonNull String message) {
        if (this.holdingSnackBar == null) {
            holdingSnackBar = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_INDEFINITE);
        }

        if (!TextUtils.isEmpty(message)) {
            holdingSnackBar.setDuration(Snackbar.LENGTH_INDEFINITE);
            holdingSnackBar.setText(message);
            holdingSnackBar.show();
        }
    }

    /**
     * 取消显示固定的Snackbar
     */
    public void dismissHoldingSnackBar() {
        if (this.holdingSnackBar != null && this.holdingSnackBar.isShown()) {
            this.holdingSnackBar.setDuration(Snackbar.LENGTH_SHORT);
            this.holdingSnackBar.dismiss();
        }
    }

    /**
     * 返回ToolBar对象
     *
     * @return ToolBar对象 , 如果没有在 onSetToolBar() 中返回资源ID , 则会返回NULL
     */
    protected
    @Nullable
    Toolbar getToolBar() {
        return this.toolbar;
    }

    /**
     * @return ToolBar是否已经加载成功
     */
    protected boolean isToolBarLoaded() {
        return toolbar != null;
    }

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
