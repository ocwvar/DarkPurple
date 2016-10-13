package com.ocwvar.darkpurple.Activities;

import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.BaseBlurActivity;
import com.ocwvar.darkpurple.widgets.VerticalSeekBar;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/12 16:41
 * File Location com.ocwvar.darkpurple.Activities
 * 均衡器设置界面
 */

public class EqualizerActivity extends BaseBlurActivity implements SeekBar.OnSeekBarChangeListener {

    VerticalSeekBar equ1, equ2, equ3, equ4, equ5, equ6, equ7, equ8, equ9, equ10;
    AudioService service;
    int[] eqParameters = null;

    @Override
    protected boolean onPreSetup() {
        //先从音频服务获取当前的均衡器参数
        service = ServiceHolder.getInstance().getService();
        eqParameters = service.getEqParameters();
        if (service == null){
            Toast.makeText(EqualizerActivity.this, R.string.eq_service_setup_error, Toast.LENGTH_SHORT).show();
            return false;
        }else {
            return true;
        }
    }

    @Override
    protected int setActivityView() {
        return R.layout.activity_equalizer;
    }

    @Override
    protected void onSetupViews() {
        equ1 = (VerticalSeekBar) findViewById(R.id.equ1);
        equ1.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ1.setProgress(eqParameters[0]);

        equ2 = (VerticalSeekBar) findViewById(R.id.equ2);
        equ2.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ2.setProgress(eqParameters[1]);

        equ3 = (VerticalSeekBar) findViewById(R.id.equ3);
        equ3.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ3.setProgress(eqParameters[2]);

        equ4 = (VerticalSeekBar) findViewById(R.id.equ4);
        equ4.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ4.setProgress(eqParameters[3]);

        equ5 = (VerticalSeekBar) findViewById(R.id.equ5);
        equ5.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ5.setProgress(eqParameters[4]);

        equ6 = (VerticalSeekBar) findViewById(R.id.equ6);
        equ6.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ6.setProgress(eqParameters[5]);

        equ7 = (VerticalSeekBar) findViewById(R.id.equ7);
        equ7.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ7.setProgress(eqParameters[6]);

        equ8 = (VerticalSeekBar) findViewById(R.id.equ8);
        equ8.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ8.setProgress(eqParameters[7]);

        equ9 = (VerticalSeekBar) findViewById(R.id.equ9);
        equ9.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ9.setProgress(eqParameters[8]);

        equ10 = (VerticalSeekBar) findViewById(R.id.equ10);
        equ10.setOnSeekBarChangeListener(EqualizerActivity.this);
        equ10.setProgress(eqParameters[9]);
    }

    @Override
    protected void onViewClick(View clickedView) {

    }

    @Override
    protected boolean onViewLongClick(View holdedView) {
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    /**
     * 当滚动条停止变动的时候,更改均衡器参数
     *
     * @param seekBar   发生变动的滚动条
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        //从TAG中获取到发生变化的位置 (在布局文件中已设置对应的每一个滚动条的TAG)
        int eqIndex = Integer.parseInt((String) seekBar.getTag());
        //获取调整的数值 , 区间在 -10 ~ 10
        int eqParameter = seekBar.getProgress() - 10;

        if (service != null){
            service.updateEqParameter(eqParameter,eqIndex);
        }
    }

}
