package com.ocwvar.darkpurple.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.ocwvar.darkpurple.Adapters.EqualizerListAdapter;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.BaseBlurActivity;
import com.ocwvar.darkpurple.Units.EqualizerHandler;
import com.ocwvar.darkpurple.widgets.EqualizerImage;

import java.lang.ref.WeakReference;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2017/8/15 14:44
 * File Location com.ocwvar.darkpurple.Activities
 * 均衡器设置界面
 */

public class EqualizerActivity extends BaseBlurActivity implements EqualizerListAdapter.Callback, EqualizerHandler.Callback, EqualizerImage.Callback {

    private String usingEqualizerName = "Default";

    private WeakReference<SavingEqzDialog> dialogKeeper = new WeakReference<>(null);
    private EqualizerListAdapter adapter;
    private EqualizerImage equalizerImage;

    @Override
    protected boolean onPreSetup() {

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(Color.argb(90, 0, 0, 0));
            getWindow().setStatusBarColor(Color.argb(0, 0, 0, 0));
        }

        return true;
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
        adapter = new EqualizerListAdapter(EqualizerActivity.this);
        adapter.putSource(EqualizerHandler.INSTANCE.savedProfilesName());

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(EqualizerActivity.this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(this.adapter);

        findViewById(R.id.equalizer_save).setOnClickListener(EqualizerActivity.this);

        //设置均衡器的回调接口，注意在Activity销毁的时候要释放对象
        EqualizerHandler.INSTANCE.setCallback(EqualizerActivity.this);
        //设置当前正在使用的配置名称
        this.usingEqualizerName = EqualizerHandler.INSTANCE.usingProfileName();
        adapter.setUsingName(this.usingEqualizerName);

        //配置均衡器控制View
        this.equalizerImage = (EqualizerImage) findViewById(R.id.equalizerImage);
        //先进行控制点的初始化，再进行值的初始化
        this.equalizerImage.initPoints(EqualizerHandler.INSTANCE.numberOfBands());
        this.equalizerImage.initLevelValues(EqualizerHandler.INSTANCE.getEqualizerArgsProfile(this.usingEqualizerName), false);
        this.equalizerImage.setCallback(EqualizerActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放回调接口
        EqualizerHandler.INSTANCE.setCallback(null);
        EqualizerHandler.INSTANCE.asyncSaveEqualizerArgs();
    }

    @Override
    protected void onViewClick(View clickedView) {
        switch (clickedView.getId()) {
            case R.id.equalizer_save:
                if (dialogKeeper.get() == null) {
                    dialogKeeper = new WeakReference<>(new SavingEqzDialog(EqualizerActivity.this));
                }
                dialogKeeper.get().show();
                break;
        }
    }

    @Override
    protected boolean onViewLongClick(View holdedView) {
        return false;
    }

    /**
     * 点击均衡器配置
     *
     * @param name     配置名称
     * @param position 点击的位置
     */
    @Override
    public void onItemClick(String name, int position) {
        if (EqualizerHandler.INSTANCE.applyEqualizerArgs(name)) {
            //应用配置成功
            this.usingEqualizerName = name;
        }
    }

    /**
     * 点击删除配置
     *
     * @param name     配置名称
     * @param position 点击的位置
     */
    @Override
    public void onDelete(String name, int position) {
        if (name.equals(this.usingEqualizerName)) {
            //如果删除的是当前正在使用的配置档，则恢复到Default档
            EqualizerHandler.INSTANCE.applyEqualizerArgs("Default");
        }
        EqualizerHandler.INSTANCE.removeSavedEqualizerArgs(name);
        this.adapter.notifyItemRemoved(position);
    }

    /**
     * 当使用的配置文件发生变化
     *
     * @param name 配置文件名称
     */
    @Override
    public void onUsingProfileChanged(String name) {
        this.adapter.setUsingName(name);
        this.usingEqualizerName = name;
    }

    /**
     * 等级发生变化回调接口
     *
     * @param index          调节位置
     * @param equalizerLevel 均衡器的等级数据
     */
    @Override
    public void onLevelChanged(short index, short equalizerLevel) {
        EqualizerHandler.INSTANCE.applyEqualizerArg(index, equalizerLevel);
    }

    /**
     * 调节结束(手动调用Level调节、停止触摸调节)后，所有的等级数据
     *
     * @param equalizerLevels 所有的等级数据
     */
    @Override
    public void onLevelChangeFinished(short[] equalizerLevels) {
        EqualizerHandler.INSTANCE.updateEqualizerArgs(this.usingEqualizerName, equalizerLevels);
    }

    /**
     * 储存均衡器配置对话框对象
     */
    private class SavingEqzDialog {

        private final EditText editText;
        private final AlertDialog alertDialog;

        private SavingEqzDialog(final @NonNull Context context) {

            this.editText = new EditText(context);
            this.editText.setHint(R.string.dialog_input_hint);
            this.editText.setSingleLine(true);
            this.editText.setTextColor(Color.WHITE);
            this.editText.setHintTextColor(Color.WHITE);
            this.editText.setBackground(null);

            this.alertDialog = new AlertDialog.Builder(context, R.style.FullScreen_TransparentBG)
                    .setView(this.editText)
                    .setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final String name = editText.getText().toString();
                            if (!TextUtils.isEmpty(name)) {
                                //进行储存
                                EqualizerHandler.INSTANCE.updateEqualizerArgs(name, equalizerImage.getAllLevels());
                                //更新列表适配器
                                adapter.putSource(EqualizerHandler.INSTANCE.savedProfilesName());
                            }
                        }
                    }).create();

        }

        void show() {
            this.alertDialog.show();
        }

        void dismiss() {
            this.alertDialog.dismiss();
        }

    }

}
