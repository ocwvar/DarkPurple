package com.ocwvar.darkpurple.Activities;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.ocwvar.darkpurple.Adapters.EqualizerSavedAdapter;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Units.BaseBlurActivity;
import com.ocwvar.darkpurple.Units.EqualizerUnits;
import com.ocwvar.darkpurple.Units.Logger;
import com.ocwvar.darkpurple.widgets.BezierView;
import com.ocwvar.darkpurple.widgets.VerticalSeekBar;

import java.lang.ref.WeakReference;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/12 16:41
 * File Location com.ocwvar.darkpurple.Activities
 * 均衡器设置界面
 */

public class EqualizerActivity extends BaseBlurActivity implements View.OnTouchListener, EqualizerUnits.OnEqualizerListChangedCallback, EqualizerSavedAdapter.OnEqualizerItemClickCallback {

    final String TAG = "均衡器";

    VerticalSeekBar equ1, equ2, equ3, equ4, equ5, equ6, equ7, equ8, equ9, equ10;
    BezierView bezierView;
    EqualizerSavedAdapter adapter;
    RecyclerView recyclerView;
    int[] eqParameters = null;

    WeakReference<AlertDialog> saveDialogTemp = new WeakReference<>(null);

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

        //重置按钮
        (findViewById(R.id.eq_reset)).setOnClickListener(this);

        //背景曲线
        bezierView = (BezierView) findViewById(R.id.bezierView);
        bezierView.setCurrentLevels(eqParameters);

        //显示已保存的下拉列表
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(EqualizerActivity.this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        adapter = new EqualizerSavedAdapter(EqualizerActivity.this);
        adapter.putNames(EqualizerUnits.getInstance().getHashMap());
        recyclerView.setAdapter(adapter);


        equ1 = (VerticalSeekBar) findViewById(R.id.equ1);
        equ1.setOnTouchListener(EqualizerActivity.this);
        equ1.setProgress(eqParameters[0] + 10);

        equ2 = (VerticalSeekBar) findViewById(R.id.equ2);
        equ2.setOnTouchListener(EqualizerActivity.this);
        equ2.setProgress(eqParameters[1] + 10);

        equ3 = (VerticalSeekBar) findViewById(R.id.equ3);
        equ3.setOnTouchListener(EqualizerActivity.this);
        equ3.setProgress(eqParameters[2] + 10);

        equ4 = (VerticalSeekBar) findViewById(R.id.equ4);
        equ4.setOnTouchListener(EqualizerActivity.this);
        equ4.setProgress(eqParameters[3] + 10);

        equ5 = (VerticalSeekBar) findViewById(R.id.equ5);
        equ5.setOnTouchListener(EqualizerActivity.this);
        equ5.setProgress(eqParameters[4] + 10);

        equ6 = (VerticalSeekBar) findViewById(R.id.equ6);
        equ6.setOnTouchListener(EqualizerActivity.this);
        equ6.setProgress(eqParameters[5] + 10);

        equ7 = (VerticalSeekBar) findViewById(R.id.equ7);
        equ7.setOnTouchListener(EqualizerActivity.this);
        equ7.setProgress(eqParameters[6] + 10);

        equ8 = (VerticalSeekBar) findViewById(R.id.equ8);
        equ8.setOnTouchListener(EqualizerActivity.this);
        equ8.setProgress(eqParameters[7] + 10);

        equ9 = (VerticalSeekBar) findViewById(R.id.equ9);
        equ9.setOnTouchListener(EqualizerActivity.this);
        equ9.setProgress(eqParameters[8] + 10);

        equ10 = (VerticalSeekBar) findViewById(R.id.equ10);
        equ10.setOnTouchListener(EqualizerActivity.this);
        equ10.setProgress(eqParameters[9] + 10);
    }

    @Override
    protected void onViewClick(View clickedView) {
        switch (clickedView.getId()) {
            case R.id.eq_reset:
                resetEqualizer();
                break;
        }
    }

    @Override
    protected boolean onViewLongClick(View holdedView) {
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {

            VerticalSeekBar seekBar = (VerticalSeekBar) v;

            //从TAG中获取到发生变化的位置 (在布局文件中已设置对应的每一个滚动条的TAG)
            int eqIndex = Integer.parseInt((String) seekBar.getTag());
            //获取调整的数值 , 区间在 -10 ~ 10
            int eqParameter = seekBar.getProgress() - 10;

            //设置曲线变化
            bezierView.setLevel(eqIndex, seekBar.getProgress());

            Logger.warning(TAG, "发生调节   位置: " + eqIndex + "   数值: " + eqParameter);


        }
        return false;
    }

    /**
     * 重置均衡器
     */
    private void resetEqualizer() {
        Snackbar.make(findViewById(android.R.id.content), R.string.eq_reset_successful, Snackbar.LENGTH_SHORT).show();
        Snackbar.make(findViewById(android.R.id.content), R.string.eq_reset_failed, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * 显示储存对话框
     */
    private void showSaveDialog() {
        AlertDialog alertDialog = saveDialogTemp.get();
        if (alertDialog == null) {

            final EditText editText = new EditText(EqualizerActivity.this);
            editText.setHint(R.string.eq_dialog_info);
            editText.setMaxLines(1);
            editText.setBackgroundColor(Color.argb(120, 0, 0, 0));
            editText.setTextSize(15f);
            editText.setHintTextColor(Color.argb(120, 255, 255, 255));
            editText.setTextColor(Color.WHITE);

            AlertDialog.Builder builder = new AlertDialog.Builder(EqualizerActivity.this, R.style.FullScreen_TransparentBG);
            builder.setView(editText);
            builder.setTitle(R.string.eq_dialog_title);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EqualizerUnits.getInstance().saveEqualizer(editText.getText().toString(), eqParameters.clone(), EqualizerActivity.this);
                    editText.getText().clear();
                    dialog.dismiss();
                }
            });

            alertDialog = builder.create();
            saveDialogTemp = new WeakReference<>(alertDialog);
        }

        alertDialog.show();
    }

    /**
     * 应用已有的均衡器配置
     *
     * @param values 配置数组
     */
    private void useEqualizerSetting(int[] values) {


    }

    @Override
    public void onSaveCompleted(String name) {
        Snackbar.make(findViewById(android.R.id.content), R.string.eq_save_completed, Snackbar.LENGTH_SHORT).show();
        adapter.putName(name);
    }

    @Override
    public void onSaveFailed(String name) {
        Snackbar.make(findViewById(android.R.id.content), R.string.eq_save_failed, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onRemoveCompleted(String name, int restCount) {
        Snackbar.make(findViewById(android.R.id.content), R.string.eq_remove_completed, Snackbar.LENGTH_SHORT).show();
        adapter.remove(name);
    }

    @Override
    public void onRemoveFailed(String name, int restCount) {
        Snackbar.make(findViewById(android.R.id.content), R.string.eq_remove_failed, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onAddItemClick() {
        showSaveDialog();
    }

    @Override
    public void onItemClick(String name) {
        final int[] values = EqualizerUnits.getInstance().getEqualizerValues(name);
        if (values != null) {
            useEqualizerSetting(values);
        }
    }

    @Override
    public void onItemRemoveClick(String name) {
        EqualizerUnits.getInstance().removeEqualizerValue(name, EqualizerActivity.this);
    }

}
