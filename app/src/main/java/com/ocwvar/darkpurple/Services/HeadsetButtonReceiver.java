package com.ocwvar.darkpurple.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ocwvar.darkpurple.AppConfigs;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/8/21 13:17
 * File Location com.ocwvar.darkpurple.Services
 * 耳机按键广播接收器
 */
public class HeadsetButtonReceiver extends BroadcastReceiver {

    static final String fromMediaSession = "compat";
    AudioService audioService;

    public HeadsetButtonReceiver() {
        audioService = ServiceHolder.getInstance().getService();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (AppConfigs.isListenMediaButton && audioService != null) {
            //如果用户允许监听耳机多媒体按钮  同时  音频服务还存活 , 则响应操作
            audioService.onMediaButtonPress(intent);
        }

    }

}
