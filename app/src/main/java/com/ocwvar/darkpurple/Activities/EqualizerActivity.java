package com.ocwvar.darkpurple.Activities;

import android.support.design.widget.Snackbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ocwvar.darkpurple.R;
import com.ocwvar.darkpurple.Services.AudioService;
import com.ocwvar.darkpurple.Services.ServiceHolder;
import com.ocwvar.darkpurple.Units.BaseBlurActivity;
import com.ocwvar.darkpurple.Units.Logger;
import com.ocwvar.darkpurple.widgets.VerticalSeekBar;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/12 16:41
 * File Location com.ocwvar.darkpurple.Activities
 * 均衡器设置界面
 */

public class EqualizerActivity extends BaseBlurActivity implements View.OnTouchListener {

    final String TAG = "均衡器";

    VerticalSeekBar equ1, equ2, equ3, equ4, equ5, equ6, equ7, equ8, equ9, equ10;
    AudioService service;
    int[] eqParameters = null;

    @Override
    protected boolean onPreSetup() {
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
        //复位键
        (findViewById(R.id.eq_reset)).setOnClickListener(this);

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
            Snackbar.make(findViewById(android.R.id.content),R.string.eq_reset_successful,Snackbar.LENGTH_SHORT).show();
        }else {
            Snackbar.make(findViewById(android.R.id.content),R.string.eq_reset_failed,Snackbar.LENGTH_SHORT).show();
        }
    }

}
