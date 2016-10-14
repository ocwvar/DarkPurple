package com.ocwvar.darkpurple.Activities;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ocwvar.darkpurple.Adapters.EqualizerSavedAdapter;
import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;
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
    AudioService service;
    BezierView bezierView;
    EqualizerSavedAdapter adapter;
    RecyclerView recyclerView;
    int[] eqParameters = null;

    WeakReference<AlertDialog> saveDialogTemp = new WeakReference<>(null);

    @Override
    protected boolean onPreSetup() {

        if (Build.VERSION.SDK_INT >= 21){
            getWindow().setNavigationBarColor(Color.argb(90,0,0,0));
        }

        //先从音频服务获取当前的均衡器参数
        service = ServiceHolder.getInstance().getService();
        eqParameters = service.getEqParameters();
        if (service == null) {
            Toast.makeText(EqualizerActivity.this, R.string.eq_service_setup_error, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected int setActivityView() {
        return R.layout.activity_equalizer;
    }

    @Override
    protected void onSetupViews() {

        //重置按钮
        (findViewById(R.id.eq_reset)).setOnClickListener(this);

        //背景曲线
        bezierView = (BezierView)findViewById(R.id.bezierView);
        bezierView.setCurrentLevels(eqParameters);

        //显示已保存的下拉列表
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(EqualizerActivity.this,LinearLayoutManager.VERTICAL,false));
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
        switch (clickedView.getId()){
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
            bezierView.setLevel(eqIndex,seekBar.getProgress());

            Logger.warnning(TAG, "发生调节   位置: " + eqIndex + "   数值: " + eqParameter);

            if (service != null) {
                Logger.warnning(TAG,"修改结果: "+service.updateEqParameter(eqParameter, eqIndex));
            }
        }
        return false;
    }

    /**
     * 重置均衡器
     */
    private void resetEqualizer(){
        if (service != null){
            equ1.setProgress(10);
            equ2.setProgress(10);
            equ3.setProgress(10);
            equ4.setProgress(10);
            equ5.setProgress(10);
            equ6.setProgress(10);
            equ7.setProgress(10);
            equ8.setProgress(10);
            equ9.setProgress(10);
            equ10.setProgress(10);
            service.resetEqualizer();
            bezierView.resetLevel();
            Snackbar.make(findViewById(android.R.id.content),R.string.eq_reset_successful,Snackbar.LENGTH_SHORT).show();
        }else {
            Snackbar.make(findViewById(android.R.id.content),R.string.eq_reset_failed,Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示储存对话框
     */
    private void showSaveDialog(){
        AlertDialog alertDialog = saveDialogTemp.get();
        if (alertDialog == null){

            final EditText editText = new EditText(EqualizerActivity.this);
            editText.setHint(R.string.eq_dialog_info);
            editText.setMaxLines(1);
            editText.setBackgroundColor(Color.argb(120, 0, 0, 0));
            editText.setTextSize(15f);
            editText.setHintTextColor(Color.argb(120,255,255,255));
            editText.setTextColor(Color.WHITE);

            AlertDialog.Builder builder = new AlertDialog.Builder(EqualizerActivity.this,R.style.FullScreen_TransparentBG);
            builder.setView(editText);
            builder.setTitle(R.string.eq_dialog_title);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EqualizerUnits.getInstance().saveEqualizer(editText.getText().toString(),eqParameters.clone(),EqualizerActivity.this);
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
     * @param values    配置数组
     */
    private void useEqualizerSetting(int[] values){

        equ1.setProgress(values[0]+10);
        bezierView.setLevel(0,equ1.getProgress());
        service.updateEqParameter(values[0],0);

        equ2.setProgress(values[1]+10);
        bezierView.setLevel(1,equ2.getProgress());
        service.updateEqParameter(values[1],1);

        equ3.setProgress(values[2]+10);
        bezierView.setLevel(2,equ3.getProgress());
        service.updateEqParameter(values[2],2);

        equ4.setProgress(values[3]+10);
        bezierView.setLevel(3,equ4.getProgress());
        service.updateEqParameter(values[3],3);

        equ5.setProgress(values[4]+10);
        bezierView.setLevel(4,equ5.getProgress());
        service.updateEqParameter(values[4],4);

        equ6.setProgress(values[5]+10);
        bezierView.setLevel(5,equ6.getProgress());
        service.updateEqParameter(values[5],5);

        equ7.setProgress(values[6]+10);
        bezierView.setLevel(6,equ7.getProgress());
        service.updateEqParameter(values[6],6);

        equ8.setProgress(values[7]+10);
        bezierView.setLevel(7,equ8.getProgress());
        service.updateEqParameter(values[7],7);

        equ9.setProgress(values[8]+10);
        bezierView.setLevel(8,equ9.getProgress());
        service.updateEqParameter(values[8],8);

        equ10.setProgress(values[9]+10);
        bezierView.setLevel(9,equ10.getProgress());
        service.updateEqParameter(values[9],9);

    }

    @Override
    public void onSaveCompleted(String name) {
        Snackbar.make(findViewById(android.R.id.content),R.string.eq_save_completed,Snackbar.LENGTH_SHORT).show();
        adapter.putName(name);
    }

    @Override
    public void onSaveFailed(String name) {
        Snackbar.make(findViewById(android.R.id.content),R.string.eq_save_failed,Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onRemoveCompleted(String name,int restCount) {
        Snackbar.make(findViewById(android.R.id.content),R.string.eq_remove_completed,Snackbar.LENGTH_SHORT).show();
        adapter.remove(name);
    }

    @Override
    public void onRemoveFailed(String name,int restCount) {
        Snackbar.make(findViewById(android.R.id.content),R.string.eq_remove_failed,Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onAddItemClick() {
        showSaveDialog();
    }

    @Override
    public void onItemClick(String name) {
        final int[] values = EqualizerUnits.getInstance().getEqualizerValues(name);
        if (values != null){
            useEqualizerSetting(values);
        }
    }

    @Override
    public void onItemRemoveClick(String name) {
        EqualizerUnits.getInstance().removeEqualizerValue(name,EqualizerActivity.this);
    }

}
